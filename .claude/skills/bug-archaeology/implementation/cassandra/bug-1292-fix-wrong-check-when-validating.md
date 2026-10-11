# Bug 1292: Wrong Check When Validating in Parallel

**Commit**: 1dff098e4c · **Ticket**: CASSANDRA-9099
**Subsystem**: src/java/org/apache/cassandra/db/compaction/CompactionManager.java
**Category**: Logic error in condition

---

## Root Cause

In `CompactionManager`, the loop that selects SSTables for parallel validation was selecting exactly the wrong set of tables. The condition used `!(intersects(...))`, which adds an SSTable to the validation set only when its token range does *not* intersect the repair range. The intent was the opposite: include SSTables that *do* intersect the range being validated. Because the boolean was inverted, repair validated tables that had no overlap with the requested range and silently skipped the tables that actually needed to be checked.

### Before / After

```java
// BEFORE — negation inverts the intended filter
if (!(new Bounds<>(sstable.first.getToken(), sstable.last.getToken())
        .intersects(Arrays.asList(validator.desc.range))))
{
    sstablesToValidate.add(sstable);
}
```

```java
// AFTER — no negation; adds sstables that intersect the repair range
if (new Bounds<>(sstable.first.getToken(), sstable.last.getToken())
        .intersects(Collections.singletonList(validator.desc.range)))
{
    sstablesToValidate.add(sstable);
}
```

---

## How to Spot

Negated predicate calls on boolean-returning methods like `intersects`, `contains`, or `matches` are a common source of inverted-logic bugs, especially when the method name already encodes the positive sense. Reviewers should read each filtering condition aloud—"add this SSTable when it does NOT intersect the range"—and verify that matches the stated intent. The pattern `!(collection.intersects(...))` or `!predicate.test(x)` on an inclusion filter is a strong code-review red flag.

---

## Test

No test added.
