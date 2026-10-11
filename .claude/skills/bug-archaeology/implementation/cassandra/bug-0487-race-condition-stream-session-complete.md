# Bug 0487: Race Condition When Completing Stream Sessions

**Commit**: 9f6fcc340d · **Ticket**: CASSANDRA-15666
**Subsystem**: `src/java/org/apache/cassandra/streaming/`
**Category**: Race condition / missing synchronization

---

## Root Cause

The streaming completion protocol had an ambiguous role model: both the initiator and the follower could send a `CompleteMessage` and then call `closeSession()`, so whichever side reached `maybeCompleted()` first could close the channels before the other side received its `CompleteMessage`. The old code used an `AtomicBoolean isAborted` to prevent double-close but did not synchronize the decision of *which* side closes channels, leading to races where channels were torn down mid-transfer. The root problem was that `isReceiving()` inferred role from `connectionsPerHost == 0` — an unreliable heuristic — rather than from an explicit flag passed at construction.

### Before / After

```java
// BEFORE — both sides could close channels; role inferred unreliably
private boolean maybeCompleted() {
    if (state == State.WAIT_COMPLETE) {
        if (!completeSent) {
            messageSender.sendMessage(new CompleteMessage());
            completeSent = true;
        }
        closeSession(State.COMPLETE);  // either side could run this
    } ...
}
// isReceiving() = connectionsPerHost == 0  (fragile heuristic)
```

```java
// AFTER — only follower sends CompleteMessage; only initiator closes channels
private synchronized boolean maybeCompleted() {
    if (maybeCompleted) return true;
    maybeCompleted = true;
    if (!isFollower) {
        // initiator: wait for follower's CompleteMessage before closing
        if (state == State.WAIT_COMPLETE) closeSession(State.COMPLETE);
        else state(State.WAIT_COMPLETE);
    } else {
        messageSender.sendMessage(new CompleteMessage());
        closeSession(State.COMPLETE);
    }
    return true;
}
// isFollower passed explicitly at construction
```

---

## How to Spot

In asymmetric protocols where one side initiates and the other follows, encoding role as a derived heuristic (e.g., "if connectionsPerHost == 0 I must be the receiver") is fragile. Whenever the completion or teardown sequence depends on role, the role should be an explicit, immutable field set at object construction. Also watch for `synchronized` missing on methods that both (a) read/write shared state and (b) can be invoked from multiple threads — here `maybeCompleted()` and `complete()` both needed the monitor.

---

## Test

`StreamingTest` (distributed test) was added to cover stream session completion between two nodes, asserting no race in the completion handshake.
