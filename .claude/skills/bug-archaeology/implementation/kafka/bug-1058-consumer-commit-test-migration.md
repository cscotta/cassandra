## Commit

6573b4ace1 — KAFKA-19042: Migrate PlaintextConsumerCommitTest from Scala to Java

## Ticket

KAFKA-19042

## Subsystem

Consumer / integration tests

## Category

Logic error in condition

## Root cause

`PlaintextConsumerCommitTest` was written in Scala and located in the `core` integration-test module, mixing test infrastructure concerns with Scala-specific idioms. Several assertions used Scala `Option.get` without guarding for `None`, which would throw `NoSuchElementException` rather than a meaningful test failure when the optional value was absent. Moving the tests to a dedicated Java `clients-integration-tests` module and rewriting them in Java makes the assertions explicit (`assertNotNull`, `assertEquals`) and removes the silent failure path.

## Before / After

```scala
// Before — Scala Option.get throws if None
val record = consumer.poll(Duration.ofMillis(5000)).asScala.headOption.get
assertEquals(record.offset(), committedOffset.offset())
```

```java
// After — explicit null guard in Java
ConsumerRecord<byte[], byte[]> record = pollForRecord(consumer);
assertNotNull(record);
assertEquals(committedOffset.offset(), record.offset());
```

## How to spot

Search for `.get` called on Scala `Option` values in test assertions. Without a preceding `isDefined` check, any absent value will produce an opaque `NoSuchElementException` instead of a descriptive assertion failure, hiding the real test intent.

## Test

Migrated tests in `PlaintextConsumerCommitTest.java` under `clients/clients-integration-tests` cover all prior Scala test scenarios.
