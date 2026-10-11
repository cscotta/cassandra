## Commit
de7e0ebb44

## Ticket
KAFKA-20267

## Subsystem
Storage / Log Cleaner

## Category
Test refactor / migration

## Root cause
`AbstractLogCleanerIntegrationTest.scala` and `LogCleanerParameterizedIntegrationTest.scala` were Scala tests living in the `core` module. As part of the ongoing migration to the `storage` module and Java, these tests were deleted and their coverage rewritten in Java under the new module. No production logic was changed.

## Before
```
core/src/test/scala/.../kafka/log/AbstractLogCleanerIntegrationTest.scala  (exists)
core/src/test/scala/.../kafka/log/LogCleanerParameterizedIntegrationTest.scala  (exists)
```

## After
Both files deleted; equivalent tests added in `storage` module as Java.

## How to spot
When migrating Scala tests to Java, ensure all parameterized test variants are replicated and the new test class is properly registered in the build. Missing a `@ParameterizedTest` variant can silently drop coverage.

## Test
The replacement Java tests in the `storage` module provide equivalent coverage.
