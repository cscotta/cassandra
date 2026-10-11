## Commit
6ceb36f98f

## Ticket
KAFKA-19752

## Subsystem
Storage / Log

## Category
Test refactor / migration

## Root cause
Continuation of the incremental migration of `UnifiedLogTest.scala` from the `core` module to Java in the `storage` module. This batch covers tests for retention idempotency, log start offset movement, snapshot lifecycle, and producer ID expiration.

## Before
Many `@Test` methods present in `core/.../UnifiedLogTest.scala`.

## After
Those methods removed from the Scala file; equivalent Java tests added in `storage` module.

## How to spot
Incremental migrations are easy to leave incomplete. Track progress by maintaining a list of all test method names and crossing them off as they are ported.

## Test
Java replacement tests in `storage` module provide identical coverage.
