# Bug 2460: ClassCastException in Hinted Handoff due to wrong Range generic type

**Commit**: a6a4beb0e4 · **Ticket**: CASSANDRA-3694

## Detection Rule

When constructing a `Range<T>` with generic bounds, verify that the type argument matches what the receiving method expects; using the wrong concrete type (e.g., `Token` instead of `RowPosition`) causes a `ClassCastException` at runtime.

## Code Smell

`new Range(p.getMinimumToken(), p.getMinimumToken(), p)` assigned to a raw `Range` and passed to `getRangeSlice()`; at runtime the range's token boundaries are cast to `RowPosition` causing a `ClassCastException`.

## Tags

- scope: cassandra
- category: wrong-serialization
- code-pattern: wrong-generic-type-argument
- severity: crash
- subsystem: gossip

## Root Cause

`HintedHandOffManager` created a full-ring range using `new Range(p.getMinimumToken(), p.getMinimumToken(), p)` with no type parameter. `getRangeSlice` expected a `Range<RowPosition>` and internally cast the endpoints to `RowPosition`. `Token` does not extend `RowPosition` in this codebase, so the cast threw `ClassCastException` during every hinted handoff delivery attempt. The fix explicitly creates a `Range<RowPosition>` using `p.getMinimumToken().minKeyBound()`.

## Example

```java
// BEFORE – raw Range, Token cast to RowPosition fails
Range range = new Range(p.getMinimumToken(), p.getMinimumToken(), p);
List<Row> rows = hintStore.getRangeSlice(null, range, ...);

// AFTER
RowPosition minPos = p.getMinimumToken().minKeyBound();
Range<RowPosition> range = new Range<RowPosition>(minPos, minPos, p);
List<Row> rows = hintStore.getRangeSlice(null, range, ...);
```
