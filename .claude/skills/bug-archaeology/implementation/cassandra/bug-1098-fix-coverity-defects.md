# Bug 1098: Fix Coverity Defects — Null Dereference and Dead Code in EncodingStats / UnfilteredSerializer

**Commit**: 80a141ce01 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/db/rows/EncodingStats.java, src/java/org/apache/cassandra/db/rows/UnfilteredSerializer.java
**Category**: Missing null / bounds check

---

## Root Cause

Two separate Coverity findings were addressed. First, in `UnfilteredSerializer`, the `data` parameter was guarded by a null check (`data == null ? DeletionTime.LIVE : data.complexDeletion()`) in the write and size paths, but `data` is only passed when `hasComplexDeletion` is true — a contract that guarantees it is non-null. The defensive null check was therefore both wrong as documentation and a potential source of confusion; removing it makes the real invariant explicit and lets Coverity (and readers) verify it is upheld. Second, `EncodingStats.Collector` carried dead fields (`isColumnSetPerRowSet`, `totalColumnsSet`, `rows`) that were written by `updateColumnSetPerRow(long, long)` but never read when building the final stats object; those fields and the two-argument overload were deleted, and the single-argument overload was emptied (the average-column-per-row stat was unused).

### Before / After

```java
// BEFORE — in UnfilteredSerializer (both write and size paths)
if (hasComplexDeletion)
    header.writeDeletionTime(data == null ? DeletionTime.LIVE : data.complexDeletion(), out);
```

```java
// AFTER
if (hasComplexDeletion)
    header.writeDeletionTime(data.complexDeletion(), out);
```

---

## How to Spot

When a null guard is nested inside a block that is only entered when a non-null invariant already holds (e.g., `hasComplexDeletion == true` implies `data != null`), the guard is a red flag: either the invariant is wrong, or the guard is misleading dead code. Similarly, fields written in `update*` methods but never read in the finalizing step of a collector are dead stores that clutter the class and hide real logic. Both classes of issue are reliably caught by static analysis.

---

## Test

No test added.
