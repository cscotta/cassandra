## Commit

- Hash: f79d7dc3f4
- Tickets: KAFKA-18035
- Subject: KAFKA-18035: Re-add @Flaky annotation to testBumpTransactionalEpochWithTV2Disabled

## Subsystem

core / integration tests / transactions

## Category

Race condition / missing synchronization

## Root cause

`TransactionsTest.testBumpTransactionalEpochWithTV2Disabled` was intermittently failing in CI due to a known timing-dependent race in Transaction V2 epoch bumping when TV2 is disabled. A previous commit had removed the `@Flaky` annotation too eagerly before the underlying race was fixed; subsequent CI runs exposed the flakiness again. The fix re-adds `@Flaky("KAFKA-18035")` so the test is tracked as known-flaky rather than blocking CI pipelines.

## Before

```java
@ParameterizedTest
@CsvSource(Array("kraft,classic,false", ...))
def testBumpTransactionalEpochWithTV2Disabled(...): Unit = { ... }
```

## After

```java
@Flaky("KAFKA-18035")
@ParameterizedTest
@CsvSource(Array("kraft,classic,false", ...))
def testBumpTransactionalEpochWithTV2Disabled(...): Unit = { ... }
```

## How to spot

Integration tests for transactional epoch logic that are parameterised across TV1/TV2 combinations — mixed-mode epoch bumping is a known source of timing races that require explicit `@Flaky` tracking until the root cause is resolved.

## Test

The annotation re-addition is the fix; no logic change. The underlying KAFKA-18035 race is tracked separately.
