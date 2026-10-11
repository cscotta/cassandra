# Bug 1701: Fix SSTable not released if stream fails before it starts

**Commit**: 35d4b5de8f · **Ticket**: CASSANDRA-6818
**Subsystem**: src/java/org/apache/cassandra/streaming/
**Category**: Resource leak

---

## Root Cause

`AbstractStreamSession` registered the session with `Gossiper` and `FailureDetector` in its constructor. These listeners are responsible for detecting node failures and cleaning up SSTable references. If a stream session was created but then failed before `begin()` / the first network transfer, the listeners were still registered from the constructor, but the cleanup path that releases SSTable file handles could be bypassed. The fix moves listener registration out of the constructor and into the concrete `create()` factory methods (`StreamInSession.create()`, `StreamOutSession.begin()`) so that listeners — and thus the cleanup callback — are only attached once the session is actually committed to starting.

### Before / After

```java
// BEFORE (AbstractStreamSession constructor)
Gossiper.instance.register(this);
FailureDetector.instance.registerFailureDetectionEventListener(this);
```

```java
// AFTER (StreamOutSession.begin(), StreamInSession.create())
// Registration deferred until the session is actually started:
Gossiper.instance.register(this);
FailureDetector.instance.registerFailureDetectionEventListener(this);
// Constructor no longer registers anything.
```

---

## How to Spot

Resource cleanup listeners should be registered at the same point the resource is acquired, not in a constructor that runs before the resource is fully committed. Review any class that registers failure or lifecycle listeners in a constructor: if object creation can succeed while the underlying resource acquisition can fail separately, the listeners should be registered alongside the resource, not at construction time.

---

## Test

No test added.
