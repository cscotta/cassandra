# Bug 1604: Fix IN Queries Missing Results with Reversed Clustering

**Commit**: 7bbeb5aa2d · **Ticket**: CASSANDRA-7105
**Subsystem**: src/java/org/apache/cassandra/cql3/statements
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

In `SelectStatement.buildBound()`, the variable `b` (the requested bound direction) was used directly when deciding whether to call `buildAsEndOfRange()` for IN-value entries. For tables with reversed clustering columns the correct end-of-range marker depends on `eocBound` (the effective bound after accounting for reversal), not the raw `bound` value. Using `bound` instead of `eocBound` caused the wrong composite key to be emitted as the slice end, which made range scans miss rows that were stored at positions only reachable via the corrected end marker.

### Before / After

```java
// BEFORE
s.add((b == Bound.END && copy.remainingCount() > 0) ? copy.buildAsEndOfRange() : copy.build());
...
return Collections.singletonList((bound == Bound.END && builder.remainingCount() > 0) ? builder.buildAsEndOfRange() : builder.build());
```

```java
// AFTER
s.add((eocBound == Bound.END && copy.remainingCount() > 0) ? copy.buildAsEndOfRange() : copy.build());
...
return Collections.singletonList((eocBound == Bound.END && builder.remainingCount() > 0) ? builder.buildAsEndOfRange() : builder.build());
```

---

## How to Spot

When a method computes a derived direction variable (`eocBound`) specifically to handle reversed clustering, every subsequent use of the original `bound` variable is a suspect. A quick search for remaining references to the raw `bound` within the same scope after `eocBound` is defined surfaces the inconsistency immediately.

---

## Test

No test added.
