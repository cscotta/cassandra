## Commit
5a628e5935

## Ticket
KAFKA-20321

## Subsystem
Consumer / Membership Manager

## Category
Race condition / missing synchronization

## Root cause
`AbstractMembershipManager.signalPartitionsLost()` invoked `signalPartitionsLost(partitionsLost)` (the callback) without first calling `markPendingRevocationToPauseFetching()`. Between callback invocation and actual revocation, the fetcher could still return buffered records for partitions that were logically lost, violating the contract that revoked partitions produce no more records.

## Before
```java
// signalPartitionsLost
CompletableFuture<Void> result = signalPartitionsLost(partitions); // callback first
result.whenComplete(...markPendingRevocation...);                  // pause after
```

## After
```java
// mark pause BEFORE callback, matching the revoke path
markPendingRevocationToPauseFetching(partitions);
CompletableFuture<Void> result = signalPartitionsLost(partitions);
```

## How to spot
Compare the "lost" code path with the "revoked" code path side-by-side. Any asymmetry in when fetching is paused relative to when the callback fires is a potential race.

## Test
`ConsumerMembershipManagerTest.testOnPartitionsLostDoesNotReturnRecords` verifies no records are returned after the callback.
