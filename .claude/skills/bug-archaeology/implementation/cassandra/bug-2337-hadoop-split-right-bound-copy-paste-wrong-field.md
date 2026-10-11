# Bug 2337: Hadoop split generation uses left bound twice instead of left and right bounds

**Commit**: 39dd855019 · **Ticket**: CASSANDRA-4259

## Detection Rule

When computing a range's right boundary position for binary search, verify that the right bound expression references the range's right field, not a copy-paste of the left field expression.

## Code Smell

Two consecutive lines computing `leftPosition` and `rightPosition` where both lines call `range.left.maxKeyBound()` — the second line should reference `range.right`.

## Tags

- scope: java
- category: wrong-constant
- code-pattern: copy-paste-wrong-field
- severity: silent-wrong-result
- subsystem: tools-clients

## Root Cause

In `SSTableReader.getPositionsForRanges()`, the right boundary `RowPosition` was computed as `range.left.maxKeyBound()` instead of `range.right.maxKeyBound()`. This caused every range split to treat the range as having zero width (left == right), returning no samples for any range and producing empty Hadoop input splits. All Hadoop jobs using Cassandra as an input source would receive no data.

## Example

```java
// BEFORE
RowPosition leftPosition = range.left.maxKeyBound();
RowPosition rightPosition = range.left.maxKeyBound();  // BUG: should be range.right
```

```java
// AFTER
RowPosition leftPosition = range.left.maxKeyBound();
RowPosition rightPosition = range.right.maxKeyBound();
```
