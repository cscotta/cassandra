# Bug 2397: Division by zero in get_slice retry estimation when all columns are tombstoned

**Commit**: edb105ded2 · **Ticket**: CASSANDRA-4000

## Detection Rule

When a retry-count formula divides by a live-count variable that could legitimately reach zero (e.g. after tombstone filtering), guard with a zero check before dividing.

## Code Smell

`int retryCount = ((count * count) / liveColumnsInRow) + 1;` — division by `liveColumnsInRow` with no guard; when all fetched columns are tombstones the variable is 0, causing `ArithmeticException`.

## Tags

- scope: cassandra
- category: off-by-one
- code-pattern: divide-by-zero-live-count
- severity: crash
- subsystem: cql-query

## Root Cause

`SliceFromReadCommand.maybeGenerateRetryCommand` estimates how many columns to request in a retry using `count² / liveColumnsInRow`. When a row exists but every column is a tombstone, `liveColumnsInRow` is 0 and the division throws `ArithmeticException`. The fix short-circuits when `liveColumnsInRow == 0` by using `count + 1` as a reasonable retry count.

## Example

```java
// BEFORE
int retryCount = ((count * count) / liveColumnsInRow) + 1;  // ArithmeticException when liveColumnsInRow == 0
```

```java
// AFTER
int retryCount = liveColumnsInRow == 0 ? count + 1
                                       : ((count * count) / liveColumnsInRow) + 1;
```
