# Bug 0106: Interval Created with Null first/last Keys Silently Corrupts IntervalTree

**Commit**: ef13e9f9bf · **Ticket**: CASSANDRA-20407
**Subsystem**: db/lifecycle, io/sstable, utils
**Category**: Missing null / bounds check

---

## Root Cause

`SSTableReader` sets `this.interval = Interval.create(first, last, this)` unconditionally even when `first` or `last` is `null` (which occurs for SSTables opened by offline tools such as scrub, where there is intentionally no first/last key). `Interval`'s constructor never checked for null, so a tree built from such readers contained `Interval` nodes with `null` min/max values. The resulting `IntervalTree` was silently un-searchable because comparisons on null keys crashed or returned nonsensical results at runtime. The fix makes `SSTableReader` store `null` for the interval when either key is absent, adds null guards to `SSTableIntervalTree.buildIntervals` to skip those entries, and adds a `checkState` assertion that the skip only occurs when `DatabaseDescriptor.isToolInitialized()` (i.e., offline-tool context). A `checkNotNull` guard was also added to `Interval`'s constructor to catch any future violations.

### Before / After

```java
// BEFORE — always creates interval, even with null keys
this.interval = Interval.create(first, last, this);
```

```java
// AFTER — null interval for offline readers
this.interval = first == null || last == null ? null : Interval.create(first, last, this);
```

```java
// AFTER — Interval constructor rejects nulls
public Interval(C min, C max, D data)
{
    checkNotNull(min, "min is null");
    checkNotNull(max, "max is null");
    ...
}
```

---

## How to Spot

When a data structure (especially a tree or sorted collection) is constructed from a list of objects where some members may be partially initialized, each member's contribution must be guarded before insertion. A pattern like "build array from `sstable.getInterval()` without null check" is dangerous whenever the class has an "offline / partial open" construction path. Look for `getInterval()` / `getFirst()` / `getLast()` call sites and verify that null returns are handled before they propagate into comparators.

---

## Test

No dedicated new test added; the fix is guarded by a `checkState` assertion that fires in production if a null-interval SSTable is encountered outside of offline-tool initialization.
