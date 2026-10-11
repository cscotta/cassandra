## Commit
fb3d6fb546

## Ticket
KAFKA-19752

## Subsystem
Storage / Log

## Category
Test refactor / migration

## Root cause
Third incremental batch of `UnifiedLogTest.scala` → Java migration. Covers tests from `testRetentionIdempotency` through `testLogRollAfterLogHandlerClosed`. The checkstyle `JavaNCSS` suppression was extended to the new Java `UnifiedLogTest.java` to accommodate its size.

## Before
Tests present in `core/.../UnifiedLogTest.scala`.

## After
Tests removed from Scala file; rewritten in `storage/.../UnifiedLogTest.java`.

## How to spot
Check the checkstyle suppressions file when a large test class is added to ensure suppression rules are updated.

## Test
Java replacement tests provide identical or superior coverage.
