## Commit
3c42359579

## Ticket
KAFKA-19752

## Subsystem
Storage / Log

## Category
Test refactor / migration

## Root cause
Tests in `core/src/test/scala/unit/kafka/log/UnifiedLogTest.scala` were incrementally migrated to Java in the `storage` module. This batch removed Scala tests for log segment deletion, snapshot lifecycle, and producer-ID expiration that had already been rewritten in Java.

## Before
Multiple `@Test` methods present in `UnifiedLogTest.scala`.

## After
Those methods deleted from the Scala file; equivalent Java tests added under `storage` module.

## How to spot
When large Scala test classes are being ported piecemeal, track which tests have been migrated using a checklist or grep comparison. A missing test is silent—always do a method-count diff between old and new files.

## Test
Replacement Java tests provide identical coverage.
