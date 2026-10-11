## Commit

9e785cee8d — KAFKA-19087: Move TransactionState to transaction-coordinator module and rewrite in Java

## Ticket

KAFKA-19087

## Subsystem

Transaction coordinator / transaction state machine

## Category

Wrong serialization / deserialization

## Root cause

`TransactionState` was defined as a Scala sealed trait in the `core` module. Java callers in the `transaction-coordinator` module had to interact with it through Scala's object system, which exposes enum-like singletons as `MODULE$` fields rather than proper Java enum constants. Pattern matching on Scala sealed traits from Java required chained `instanceof` checks with no exhaustiveness guarantee, making it easy to miss a new state in a switch. Moving `TransactionState` to a Java enum in `transaction-coordinator` gives Java callers type-safe exhaustive switch expressions and removes the cross-language sealed-trait interaction.

## Before / After

```scala
// Before — Scala sealed trait, Java callers use instanceof chains
sealed trait TransactionState
case object Empty extends TransactionState
case object Ongoing extends TransactionState
```

```java
// After — Java enum, exhaustive switch in Java callers
public enum TransactionState {
    EMPTY, ONGOING, PREPARE_COMMIT, PREPARE_ABORT,
    COMPLETE_COMMIT, COMPLETE_ABORT, DEAD, PREPARE_EPOCH_FENCE;
}
```

## How to spot

Search for Scala sealed traits that are referenced from Java coordinator modules. Any Java code that checks `instanceof EmptyState$.MODULE$` instead of comparing to a Java enum constant is fragile: new states added to the sealed trait will not cause a compile error in Java callers, silently falling through to a default case.

## Test

`TransactionStateTest.java` was added to verify all state transitions with Java enum constants, matching the coverage of the previous `TransactionStateTest.scala`.
