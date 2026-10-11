# Bug 0525: Kafka Connect UUID mismatch check logged warning but did not skip commit

**Commit**: b2696b9599df762feb3749eef2383b7698437374 · **Ticket**: #15011

## Detection Rule

When a guard condition is added to skip or reject an operation and the body only logs a warning, verify that the block ends with a `return` (or `continue`/`throw`) so the operation is actually aborted.

## Code Smell

An `if` block that calls `LOG.warn(...)` about a mismatch condition but has no `return` statement, so execution falls through to the operation that was supposed to be skipped.

## Tags

- scope: universal
- category: logic-error
- code-pattern: missing-return-after-guard
- severity: silent-wrong-result
- subsystem: kafka-connect/channel/Coordinator

## Root Cause

`Coordinator.commitToTable()` gained a UUID mismatch check that logged a warning but omitted the `return` statement, so all commits proceeded regardless of whether the table UUID matched. Additionally, the check used `Objects.equals(table.uuid(), tableReference.uuid())` which would produce a false-positive mismatch warning for older workers that did not send a UUID (where `tableReference.uuid()` is `null`). The fix adds `return` to actually abort the commit and guards the check with `tableReference.uuid() != null`.

## Example

```java
// BEFORE
if (!Objects.equals(table.uuid(), tableReference.uuid())) {
  LOG.warn(
      "Skipping commits to table {} due to target table mismatch.  Expected: {} Received: {}",
      tableIdentifier,
      table.uuid(),
      tableReference.uuid());
  // missing return — execution falls through to the commit
}
String branch = config.tableConfig(tableIdentifier.toString()).commitBranch();
```

```java
// AFTER
if (tableReference.uuid() != null && !tableReference.uuid().equals(table.uuid())) {
  LOG.warn(
      "Skipping commits to table {} due to target table mismatch.  Expected: {} Received: {}",
      tableIdentifier,
      table.uuid(),
      tableReference.uuid());
  return;  // actually aborts the commit
}
String branch = config.tableConfig(tableIdentifier.toString()).commitBranch();
```
