# Bug 2407: Short-read protection retries with inflated count, trimming to wrong column total

**Commit**: 8413d831d1 · **Ticket**: CASSANDRA-3934

## Detection Rule

When a read-retry mechanism tracks both an inflated request count and the original user-requested count, trimming and retry-stop logic must always compare against the original count, not the inflated one.

## Code Smell

`RetriedSliceFromReadCommand` overrides `getRequestedCount()` to return `originalCount`, but the base class uses `getRequestedCount()` for both retry-generation and column-trimming; when the retry itself generates a further retry it passes `count` (the inflated value) as the `originalCount` of the new retry, silently inflating the user-visible result.

## Tags

- scope: cassandra
- category: logic-error
- code-pattern: retry-count-vs-original-count-confusion
- severity: silent-wrong-result
- subsystem: cql-query

## Root Cause

`SliceFromReadCommand.maybeGenerateRetryCommand` compared `liveColumnsInRow < count` (where `count` is the per-attempt inflated fetch size) and constructed a new `RetriedSliceFromReadCommand` with `count` as the `originalCount`. This meant that on a second retry, the "original requested" count was the inflated fetch count from the first retry rather than the user's original `count`. Column trimming used this inflated original, returning more columns than the user asked for. The fix renames `getRequestedCount()` to `getOriginalRequestedCount()` to make the intent explicit and changes the retry-generation logic to compare against and pass `getOriginalRequestedCount()`.

## Example

```java
// BEFORE — compares and propagates inflated count as original
if ((maxLiveColumns == count) && (liveColumnsInRow < count)) {
    int retryCount = count + count - liveColumnsInRow;
    return new RetriedSliceFromReadCommand(..., count, retryCount);  // count passed as originalCount
}
if (liveColumnsInRow > getRequestedCount()) { ... trim ... }
```

```java
// AFTER — always uses original user-requested count
if ((maxLiveColumns == count) && (liveColumnsInRow < getOriginalRequestedCount())) {
    int retryCount = liveColumnsInRow == 0 ? count + 1 : ((count * count) / liveColumnsInRow) + 1;
    return new RetriedSliceFromReadCommand(..., getOriginalRequestedCount(), retryCount);
}
if (liveColumnsInRow > getOriginalRequestedCount()) { ... trim ... }
```
