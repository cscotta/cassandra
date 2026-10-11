# Bug 1078: Fix IndexOutOfBoundsException When Using Empty BATCH Statement

**Commit**: 1a2a1d2c24 · **Ticket**: CASSANDRA-10277
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/BatchStatement.java
**Category**: Missing null / bounds check

---

## Root Cause

When preparing a `BATCH` statement, `BatchStatement` attempted to read `statements.get(0)` to retrieve partition key bind indexes for the response metadata. The code only skipped this call when the batch spanned multiple tables (`haveMultipleCFs`), but did not guard against an empty `statements` list. Submitting a syntactically valid but empty `BEGIN BATCH APPLY BATCH` statement caused an `IndexOutOfBoundsException` because `get(0)` was called on an empty list.

### Before / After

```java
// BEFORE
Short[] partitionKeyBindIndexes = haveMultipleCFs ? null
    : boundNames.getPartitionKeyBindIndexes(batchStatement.statements.get(0).cfm);
```

```java
// AFTER
Short[] partitionKeyBindIndexes = (haveMultipleCFs || batchStatement.statements.isEmpty()) ? null
    : boundNames.getPartitionKeyBindIndexes(batchStatement.statements.get(0).cfm);
```

---

## How to Spot

Any call to `list.get(0)` or `list.get(n)` that is guarded only by a semantic condition (e.g., "multiple tables") rather than a size check is vulnerable to the empty-collection case. Reviewers should verify that every first-element access has an explicit `isEmpty()` or size guard, especially in parsing / preparation code paths that accept user-supplied input.

---

## Test

`BatchTest.testBatchEmpty` was added, executing `BEGIN BATCH APPLY BATCH` to confirm no exception is thrown.
