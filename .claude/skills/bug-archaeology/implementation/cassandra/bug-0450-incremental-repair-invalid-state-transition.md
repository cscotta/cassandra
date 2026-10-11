# Bug 0450: Invalid State Transition in Incremental Repair CoordinatorSession

**Commit**: da3806795f · **Ticket**: CASSANDRA-16067
**Subsystem**: `src/java/org/apache/cassandra/repair/consistent/CoordinatorSession.java`
**Category**: Race condition / missing synchronization

---

## Root Cause

`CoordinatorSession.handlePrepareResponse()` is `synchronized`, but it did not check whether the session had already transitioned to `FAILED` before trying to process a late-arriving participant response. If one participant's prepare failed early — moving the session to `FAILED` — a second participant's prepare response could still arrive and attempt a state transition from `FAILED`, which is an invalid transition and throws an exception. The session state machine did not tolerate concurrent (re-)entry through a state that was already terminal.

### Before / After

```java
// BEFORE
public synchronized void handlePrepareResponse(InetAddressAndPort participant, boolean success)
{
    if (!success)
    {
        logger.warn("{} failed the prepare phase...", participant, sessionID);
        ...
    }
```

```java
// AFTER
public synchronized void handlePrepareResponse(InetAddressAndPort participant, boolean success)
{
    if (getState() == State.FAILED)
    {
        logger.trace("Incremental repair {} has failed, ignoring prepare response from {}", sessionID, participant);
        return;
    }
    if (!success)
    {
        logger.warn("{} failed the prepare phase...", participant, sessionID);
        ...
    }
```

---

## How to Spot

State-machine handlers that are individually synchronized should always re-check the current state at the top of the method before acting, especially in multi-participant protocols where multiple concurrent callers may legally try to drive the machine after it has already reached a terminal state. A missing terminal-state guard is the typical shape of this bug.

---

## Test

No test added.
