**Commit**: c28f46459a · **Ticket**: KAFKA-18345
**Subsystem**: raft / KafkaRaftClient
**Category**: Logic error in condition

**Root cause**
`binaryExponentialElectionBackoffMs` used `random.nextInt(2 << Math.min(20, retries-1))` which, after enough retries, always returns a value saturating at `electionBackoffMaxMs`. Multiple replicas that have all exceeded the saturation point therefore compute identical backoff windows and simultaneously attempt a new election — a livelock. The fix caps the exponential at 10 retries (not 20) to allow the first `Math.min` to dominate earlier, and adds a jitter of up to `RETRY_BACKOFF_BASE_MS` to the ceiling, ensuring that at maximum backoff different replicas diverge.

**Before/After**
```java
// Before — saturates to exactly electionBackoffMaxMs, no jitter
return Math.min(
    RETRY_BACKOFF_BASE_MS * random.nextInt(2 << Math.min(20, retries - 1)),
    quorumConfig.electionBackoffMaxMs()
);

// After — adds jitter to break synchronization
return Math.min(
    backoffBaseMs * random.nextInt(1, 2 << Math.min(10, retries - 1)),
    backoffMaxMs + random.nextInt(backoffBaseMs)
);
```

**How to spot**
Whenever a bounded random backoff can reach an exact maximum and multiple actors run the same code, all actors that have exceeded the saturation threshold will pick the same value with probability 1. Jitter must be applied to or above the ceiling, not just below it.

**Test**
`RaftUtilTest.testExponentialElectionBackoffMsIsCapped` and `testExponentialBoundOfExponentialElectionBackoffMs` — parameterized over retry counts; verify that the bound passed to `random.nextInt` stays within expected limits and that the jitter term is always included.
