# Bug 0888: Paxos Clock Skew Corrupts Node-Wide Timestamp Counter

**Commit**: 720870bd7f · **Ticket**: CASSANDRA-11991
**Subsystem**: Paxos / CAS
**Category**: Race condition / missing synchronization

---

## Root Cause
When a Paxos prepare response carried a `minTimestampToUse` from a node with a clock in the future, `getTimestamp(long)` would unconditionally CAS that future value into `lastTimestampMicros`, the global atomic used for all writes (not just Paxos ones). Any subsequent non-Paxos insert would then inherit the artificially high timestamp, so a single node with a skewed clock could corrupt the logical clock of every other node that coordinated a Paxos round against it. The fix renames the method to `getTimestampForPaxos` and skips the CAS update when the chosen timestamp equals `minTimestampToUse`, preventing the foreign clock value from propagating into the global counter. Separately, a new `getRandomTimeUUIDFromMicros` method (using `SecureRandom` for the clock-and-node field) ensures that repeated Paxos proposals with the same microsecond timestamp still produce distinct UUIDs.

### Before / After
```java
// BEFORE — always CAS the chosen value, even when it came from a future remote clock
if (lastTimestampMicros.compareAndSet(last, tstamp))
    return tstamp;

// AFTER — skip CAS when tstamp is the remote minimum, keeping local clock uncontaminated
if (tstamp == minTimestampToUse || lastTimestampMicros.compareAndSet(last, tstamp))
    return tstamp;
```

---

## How to Spot
In any code that updates a global atomic from an externally supplied value, ask whether the caller can supply a value that is larger than the local clock and whether writing that value into the global state would affect unrelated subsystems. Here the red flag is that the same `lastTimestampMicros` field is used both for normal CQL writes and for Paxos ballot generation; accepting a remote future timestamp into it creates an invisible coupling between Paxos round-trip latency and the correctness of ordinary write timestamps.

---

## Test
No test added.
