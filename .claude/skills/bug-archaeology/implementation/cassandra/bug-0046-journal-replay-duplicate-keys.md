# Bug 0046: Journal Replay Visits Same Key Multiple Times

**Commit**: 0936286ebd · **Ticket**: CASSANDRA-20675
**Subsystem**: journal/Journal, service/accord/AccordJournal, AccordJournalTable
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

Two related bugs caused the same key to be processed more than once during journal replay. First, `StaticSegmentKeyIterator` used a plain `Iterator<K>` from each segment's index, but each segment index can contain duplicate entries for the same key (multiple records for one key in the same segment). The merge iterator over these segment iterators would then emit the same logical key multiple times via the `Reducer`. Second, the `KeyRefs.add()` method previously enforced strict ascending order for segment timestamps, which was too strict and could also throw on valid inputs. The fix adds deduplication inside the per-segment iterator using `AbstractIterator`, sorts the accumulated segment timestamps after merge via `ensureSorted()`, and adds an invariant check in `AccordJournal` to detect duplicates. `waitForQuiescence` is also fixed to also wait for MUTATION stage tasks, ensuring all in-flight writes are drained before replay starts.

### Before / After

```java
// BEFORE — raw iterator, emits duplicate keys from same segment
Iterator<K> iter = staticSegment.index().reader();
Head head = new Head(staticSegment.descriptor.timestamp);
iterators.add(new Iterator<>() {
    public Head next() { head.key = iter.next(); return head; }
    ...
});
```

```java
// AFTER — AbstractIterator skips consecutive duplicates within a segment
iterators.add(new AbstractIterator<>() {
    final Iterator<K> iter = staticSegment.index().reader();
    final Head head = new Head(staticSegment.descriptor.timestamp);
    protected Head computeNext() {
        if (!iter.hasNext()) return endOfData();
        K next = iter.next();
        while (next.equals(head.key)) {
            if (!iter.hasNext()) return endOfData();
            next = iter.next();
        }
        head.key = next;
        return head;
    }
});
```

---

## How to Spot

Merge iterators that combine multiple per-segment iterators require each constituent iterator to be strictly deduplicated (no repeated equal keys) before merging, otherwise the merge reducer receives the same logical key multiple times and produces incorrect combined results. When building a merge over segment-level iterators, verify that each source iterator returns each key at most once and that the merge comparator only compares on the key dimension, not on secondary segment/position coordinates.

---

## Test

`AccordJournal` replay now asserts `txnId.compareTo(prev) != 0` for consecutive keys; no dedicated unit test added for this specific replay path.
