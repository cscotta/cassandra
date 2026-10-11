# Bug 2081: Off-by-One in LeveledManifest Bounds Check

**Commit**: 2e63d096c3 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/db/compaction/LeveledManifest.java
**Category**: Off-by-one

---

## Root Cause

`getLevelSize(int i)` guarded against out-of-bounds access with `i > generations.length`, but valid indices run from `0` to `generations.length - 1`. Passing `i == generations.length` (one past the end) satisfied the old guard and went on to call `generations[i].size()`, causing an `ArrayIndexOutOfBoundsException` at runtime. The correct guard is `i >= generations.length`.

### Before / After

```java
// BEFORE
if (i > generations.length)
    throw new ArrayIndexOutOfBoundsException("Maximum valid generation is " + (generations.length - 1));
return generations[i].size();
```

```java
// AFTER
if (i >= generations.length)
    throw new ArrayIndexOutOfBoundsException("Maximum valid generation is " + (generations.length - 1));
return generations[i].size();
```

---

## How to Spot

Any bounds guard that throws on `> length` instead of `>= length` (or equivalently `> length - 1`) will silently allow an access at index `length` — the classic fencepost error. During review, verify that the exception message's stated maximum matches exactly what the guard allows through.

---

## Test

No test added.
