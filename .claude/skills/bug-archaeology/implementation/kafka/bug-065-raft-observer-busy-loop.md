**Commit**: 833e25f015 · **Ticket**: KAFKA-19605
**Subsystem**: raft/src/main/java/org/apache/kafka/raft/KafkaRaftClient.java
**Category**: Logic error in condition

**Root cause**
`KafkaRaftClient.pollFollower()` and `pollFollowerAsObserver()` both included `state.remainingUpdateVoterSetPeriodMs()` in a `Math.min()` with the fetch backoff. For broker observers (which can never auto-join the voter set) and controller observers without auto-join support, this timer was irrelevant. Because `remainingUpdateVoterSetPeriodMs` could expire to 0 before the next `updateVoterSet` action was possible, the `Math.min` reduced the backoff to 0 on every poll iteration, creating a tight busy loop that wasted CPU.

**Before/After**
```java
// Before: updateVoterSetPeriodMs always minimised the backoff, causing busy-loop
return Math.min(backoffMs,
    Math.min(state.remainingFetchTimeMs(currentTimeMs),
             state.remainingUpdateVoterSetPeriodMs(currentTimeMs)));

// After: only the fetch timer controls the backoff for observers
return Math.min(backoffMs, state.remainingFetchTimeMs(currentTimeMs));
```

**How to spot**
When computing a sleep/backoff as the minimum of several timers, verify that each timer actually controls an action that will be taken in the current role. Including a timer for an action that is conditionally disabled (like voter-set updates for observers) causes the minimum to collapse to 0 whenever that timer fires.

**Test**
No new test was added; the fix is validated by the absence of the busy-loop in integration scenarios described in the ticket.
