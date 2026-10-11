# Bug 2382: NPE in get_slice when row exists but has null ColumnFamily

**Commit**: 5cde93d960 · **Ticket**: CASSANDRA-4095

## Detection Rule

When a `Row` object may have a `null` `cf` field (representing a row key that exists in the token range but has no live data), always call a null-safe accessor rather than dereferencing `row.cf` directly.

## Code Smell

`row.cf.getLiveColumnCount()` accessed without a null guard: if a row was returned with `cf == null` (tombstone-only or empty partition), this throws a NPE mid-read repair or retry-command generation.

## Tags

- scope: cassandra
- category: missing-null-check
- code-pattern: null-cf-field-deref
- severity: crash
- subsystem: storage-engine

## Root Cause

`SliceFromReadCommand.maybeGenerateRetryCommand()` computed `liveColumnsInRow` as `row.cf.getLiveColumnCount()` with only a null check on `row` itself. A `Row` can have a non-null key but a null `cf` (when the partition has no live columns, e.g., all tombstones). In that case, `row.cf` is null and the direct dereference throws a NPE. The fix adds a `getLiveColumnCount()` helper to `Row` that returns 0 when `cf == null` and calls that instead.

## Example

```java
// BEFORE
int liveColumnsInRow = row != null ? row.cf.getLiveColumnCount() : 0;
// NPE when row != null but row.cf == null
```

```java
// AFTER (Row.java)
public int getLiveColumnCount() {
    return cf == null ? 0 : cf.getLiveColumnCount();
}
// SliceFromReadCommand:
int liveColumnsInRow = row != null ? row.getLiveColumnCount() : 0;
```
