# Bug 0320: Race Condition Between Repair Session State Check and State Update

**Commit**: fd5e2ad74e · **Ticket**: CASSANDRA-17335
**Subsystem**: repair/consistent/LocalSessions
**Category**: Race condition / missing synchronization

---

## Root Cause

In `LocalSessions.prepareSession`, the anticompaction callback checked `session.getState() != FAILED` and then called `setStateAndSave(session, PREPARED)` as two separate, unsynchronized operations. A concurrent thread could call `failSession` on the same `LocalSession` object and change the state to `FAILED` in between the read and the write, allowing the session to be incorrectly transitioned into `PREPARED` after it had already been marked failed.

The fix extracts the check-and-set into a new helper method `prepareSessionExceptFailed` that executes both operations inside a `synchronized (session)` block, making the read-modify-write atomic.

### Before / After

```java
// BEFORE
if (session.getState() != FAILED)
    setStateAndSave(session, PREPARED);
else
    logger.info("Session {} failed before anticompaction completed", sessionID);
```

```java
// AFTER
private boolean prepareSessionExceptFailed(LocalSession session) {
    synchronized (session)
    {
        if (session.getState() == FAILED)
            return false;
        setStateAndSave(session, PREPARED);
        return true;
    }
}
```

---

## How to Spot

Any time a method reads a shared mutable field and then writes it in a conditional branch without holding the same lock for both operations, a TOCTOU (time-of-check/time-of-use) race exists. During review, look for patterns where `getState()` (or any non-synchronized getter) is followed by a non-atomic conditional write to the same object; if two threads can legally call either branch concurrently, the guard must be inside a synchronized block covering both the read and the write.

---

## Test

No test added; the fix relies on synchronization correctness rather than an observable assertion.
