# Bug 1632: Fix marking commitlog segments clean

**Commit**: 7da562053f · **Ticket**: CASSANDRA-6959
**Subsystem**: src/java/org/apache/cassandra/db/commitlog
**Category**: Logic error in condition

---

## Root Cause

`CommitLogSegment` tracks dirty column families (CFs) per segment by recording the position of the last write (`cfDirty`) and the position when a flush is marked clean (`cfClean`). When deciding whether a CF can be removed from the dirty set, the condition used strict less-than (`<`) to compare `dirtyPos < cleanPos`, which meant a CF was not cleaned when both positions were equal—i.e., when the last write position exactly matched the clean position. A segment that was fully flushed but whose final write landed at exactly the clean boundary would remain incorrectly marked dirty, preventing the segment from being recycled.

Additionally, `removeCleanFromDirty()` was called unconditionally before the early-exit guard in `getDirtyCFIDs()`, causing unnecessary work even when one of the maps was empty.

### Before / After

```java
// BEFORE
if (dirtyPos != null && dirtyPos.intValue() < cleanPos.intValue())
{
    cfDirty.remove(cfId);
    iter.remove();
}
```

```java
// AFTER
if (dirtyPos != null && dirtyPos.intValue() <= cleanPos.intValue())
{
    cfDirty.remove(cfId);
    iter.remove();
}
```

---

## How to Spot

When a boundary comparison guards a removal/cleanup operation, verify whether equality should also trigger the cleanup. Off-by-one errors on `<` vs `<=` in position/offset comparisons are a common source of "stuck" resource release in log-structured storage.

---

## Test

No test added.
