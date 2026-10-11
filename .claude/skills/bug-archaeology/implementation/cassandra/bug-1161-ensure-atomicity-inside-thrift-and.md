# Bug 1161: Race Condition in Thrift and Stream Session Map Initialization

**Commit**: 0a4728f62b · **Ticket**: CASSANDRA-7757
**Subsystem**: streaming/StreamSession.java, thrift/ThriftSessionManager.java
**Category**: Race condition / missing synchronization

---

## Root Cause

Both `StreamSession` and `ThriftSessionManager` used a check-then-act pattern to lazily populate a `ConcurrentHashMap`: check whether an entry exists, create one if absent, then put it. Because `ConcurrentHashMap` guarantees thread-safety for individual operations but not for compound read-check-write sequences, two threads could simultaneously find no entry, each construct a new object, and one would silently overwrite the other. The field declarations held the wider `Map<K,V>` interface type, which obscured the availability of `putIfAbsent` — the atomic alternative provided by `ConcurrentHashMap` itself.

### Before / After

```java
// BEFORE (StreamSession)
private final Map<UUID, StreamTransferTask> transfers = new ConcurrentHashMap<>();
...
StreamTransferTask task = transfers.get(cfId);
if (task == null)
{
    task = new StreamTransferTask(this, cfId);
    transfers.put(cfId, task);
}
```

```java
// AFTER (StreamSession)
private final ConcurrentHashMap<UUID, StreamTransferTask> transfers = new ConcurrentHashMap<>();
...
StreamTransferTask task = transfers.get(cfId);
if (task == null)
{
    StreamTransferTask newTask = new StreamTransferTask(this, cfId);
    task = transfers.putIfAbsent(cfId, newTask);
    if (task == null)
        task = newTask;
}
```

---

## How to Spot

When a field is declared as `Map` but initialized with `ConcurrentHashMap`, reviewers should ask whether compound operations such as get-then-put are protected. Declaring the variable with the concrete `ConcurrentHashMap` type makes `putIfAbsent` visible at the call site and signals the intent to use atomic operations. Any code that reads from a concurrent map, conditionally constructs an object, and then unconditionally calls `put` is a classic TOCTOU race; the canonical fix is `putIfAbsent` or `computeIfAbsent`.

---

## Test

No test added.
