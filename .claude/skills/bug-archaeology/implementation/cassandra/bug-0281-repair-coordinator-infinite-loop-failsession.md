# Bug 0281: Infinite Loop in Repair Coordinator on FailSession

**Commit**: 0353df7542 · **Ticket**: CASSANDRA-17834
**Subsystem**: repair/consistent (CoordinatorSession, LocalSessions)
**Category**: State not cleaned up

---

## Root Cause

In incremental repair, `CoordinatorSession.fail()` called `setAll(State.FAILED)` unconditionally. If any participant was already in a terminal state that could not transition to `FAILED` (e.g. `FINALIZED`), `setAll` would throw, leave the session in a partially-failed state, and the repair coordinator would retry `fail()` in a loop, never making progress. Separately, `LocalSessions.failSession()` did not guard against transitioning a session already in `FINALIZED` state, allowing it to corrupt the finalised record. Both sites needed to detect and abort the impossible transition instead of retrying.

### Before / After

```java
// BEFORE — CoordinatorSession.fail()
logger.info("Incremental repair session {} failed", sessionID);
sendFailureMessageToParticipants();
setAll(State.FAILED);  // throws if any participant already FINALIZED
```

```java
// AFTER — CoordinatorSession.fail()
Set<Map.Entry<InetAddressAndPort, State>> cantFail = participantStates.entrySet()
        .stream()
        .filter(entry -> !entry.getValue().canTransitionTo(State.FAILED))
        .collect(Collectors.toSet());
if (!cantFail.isEmpty())
{
    logger.error("Can't transition endpoints {} to FAILED", cantFail, new RuntimeException());
    return;   // abort instead of looping
}
```

---

## How to Spot

Any method that unconditionally calls a bulk state-transition helper (`setAll`, `setState`) without first checking whether every participant can accept the new state is susceptible. During review, look for state-machine methods that have no guard against already-terminal states; an invariant comment such as "only callable once" is a signal that a guard is required.

---

## Test

`IncRepairCoordinatorErrorTest` drops `FINALIZE_COMMIT_MSG` to node 3, runs repair to produce a FINALIZED session on other nodes, then calls `failSession` on node 3 and asserts the log message "Can't transition endpoints .* to FAILED" appears rather than an infinite loop.
