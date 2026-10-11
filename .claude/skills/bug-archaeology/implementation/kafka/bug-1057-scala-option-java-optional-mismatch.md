## Commit

cff10e6541 — KAFKA-19302: Fix Option[Long] vs Optional[Long] type mismatch in Replica.isBrokerEpochIsrEligible

## Ticket

KAFKA-19302

## Subsystem

Replication / replica state management

## Category

Wrong serialization / deserialization

## Root cause

The Scala `Replica` class stored broker epoch as `Option[Long]` (Scala optional) while the broader server codebase had migrated to Java `Optional[Long]`. In `isBrokerEpochIsrEligible`, the comparison between the stored Scala `Option` and the expected Java `Optional` always evaluated in an unexpected way because the two option types are not interchangeable — a Scala `None` is not equal to a Java `Optional.empty()`. This silently prevented brokers with no recorded epoch from being considered ISR-eligible. The fix migrates `Replica` and `ReplicaState` to Java, using `OptionalLong` throughout, eliminating the cross-language option confusion.

## Before / After

```scala
// Before — Scala Option, incompatible with Java callers
var brokerEpoch: Option[Long] = None
def isBrokerEpochIsrEligible(epoch: Long): Boolean =
  brokerEpoch.forall(_ == epoch)
```

```java
// After — Java OptionalLong, consistent with the rest of the server
OptionalLong brokerEpoch = OptionalLong.empty();
boolean isBrokerEpochIsrEligible(long epoch) {
    return brokerEpoch.isEmpty() || brokerEpoch.getAsLong() == epoch;
}
```

## How to spot

Search for mixed Scala `Option` and Java `Optional` usage in classes that are called from both Scala and Java contexts. Any method that compares or passes between the two option types without explicit conversion will silently misbehave.

## Test

`ReplicaTest` (Java) was added to cover `isBrokerEpochIsrEligible` with present and absent epoch values.
