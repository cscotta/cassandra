## Commit

- Hash: 15e938ef43
- Tickets: KAFKA-17973
- Subject: KAFKA-17973: Relax VoterSetHistory overlapping-majority invariant

## Subsystem

raft / VoterSetHistory

## Category

Logic error in condition

## Root cause

`VoterSetHistory.addVoterSet()` enforced a strict "overlapping-majority" invariant by throwing `IllegalArgumentException` whenever successive voter sets did not share a majority. This was overly strict for the single-broker bootstrap case and for joint-consensus scenarios where the invariant can be legitimately crossed. The exception caused KRaft bootstrapping to fail in valid configurations.

## Before

```java
if (!newSet.hasOverlappingMajority(lastSet)) {
    throw new IllegalArgumentException(
        "New voter set " + newSet + " does not have an overlapping majority " +
        "with previous voter set " + lastSet);
}
```

## After

```java
if (!newSet.hasOverlappingMajority(lastSet)) {
    logger.info("New voter set {} does not have an overlapping majority " +
        "with previous voter set {}", newSet, lastSet);
}
```

## How to spot

Hard throws on invariant violations in quorum membership transitions that are supposed to be advisory warnings rather than fatal failures. Look for `IllegalArgumentException` in raft membership code where the violation is expected during bootstrap or reconfiguration.

## Test

`VoterSetHistoryTest` updated: the non-overlapping case now asserts no exception is thrown and that a log message is emitted instead.
