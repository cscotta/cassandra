# Bug 0612: Unused Variable Reads Stale State and ConcurrentModificationException in Gossip Iterator

**Commit**: 3891598e61 · **Ticket**: CASSANDRA-7544
**Subsystem**: `src/java/org/apache/cassandra/service/StorageService.java`
**Category**: Race condition / missing synchronization

---

## Root Cause

Two independent bugs were introduced together. First, inside a synchronized block that reads `entry.getValue().getApplicationState(STATUS)` into a local variable `value`, the code then called `splitValue()` on the raw `entry.getValue().getApplicationState(STATUS)` expression again rather than on `value`, defeating the purpose of capturing the state into a local variable and re-reading a potentially changed state. Second, `replicatingNodes` was declared as `Collections.synchronizedSet(new HashSet<>())`, which does not support concurrent iteration without holding the set's intrinsic lock; any iterator obtained from it while another thread mutates the set will throw a `ConcurrentModificationException`. Replacing it with `Sets.newConcurrentHashSet()` (backed by `ConcurrentHashMap`) provides a fail-safe iterator.

### Before / After

```java
// BEFORE
private final Set<InetAddressAndPort> replicatingNodes =
    Collections.synchronizedSet(new HashSet<InetAddressAndPort>());
...
String[] pieces = splitValue(
    entry.getValue().getApplicationState(ApplicationState.STATUS));
```

```java
// AFTER
private final Set<InetAddressAndPort> replicatingNodes =
    Sets.newConcurrentHashSet();
...
String[] pieces = splitValue(value);  // uses the already-captured local variable
```

---

## How to Spot

Look for patterns where a value is read into a local variable inside a synchronized block but the original expression is used again later in the same block — this is usually a copy-paste oversight. Separately, `Collections.synchronizedSet` is safe for individual operations but not for iteration; any code that iterates such a set without holding its lock is a CME waiting to happen. Prefer `ConcurrentHashMap.newKeySet()` or `Sets.newConcurrentHashSet()` when iteration concurrency is needed.

---

## Test

No test added.
