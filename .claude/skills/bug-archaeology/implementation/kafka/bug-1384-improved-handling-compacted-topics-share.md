**Commit**: df5839a9f4 · **Ticket**: KAFKA-17351
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
The PR handles fetch for `compacted` topics. The fix was required only
when complete batch disappears from the topic log, and same batch is
marked re-available in Share Partition state cache. Subsequent log reads
will not result the disappeared batch in read response hence respective
batch will be left as available in the state cache. The PR checks for the first fetched/read batch base offset and if it's
greater than the position from where the read occurred (fetch offset)
then if there exists any `available` batches in the state cache then
they will be archived.

**Before/After**
```java
// Before
*    * @param fetchOffset        The fetch offset for which the records are fetched.
        long fetchOffset /* TODO: Use fetch offset to archive any stale batches due to compaction */,

// After
     * <p>
     * The method always acquire the full batch records. The cache state can consist of multiple
     * full batches as a single batch. This behavior is driven by client configurations (batch size
     * and max fetch records) and allows for efficient client acknowledgements. However, partial batches
     * can exist in the cache only after a leader change and partial acknowledgements have been persisted
     * prior leader change. In such case, when a share partition loses track of a batch's start and
     * end offsets (e.g., after a leader change and partial acknowledgements), the cache stores the
     * batch based on the offset range provided by the persister. This method handles these special
     * batches by maintaining this range up to the last offset returned by the persister.  No special
     * handling is required afterward; the cache will eventually return to managing full batches.
     * <p>
     * For compacted topics, batches may be non-contiguous, and records within cached batches may contain gaps.
```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17351: improved handling of compacted topics in share partition (2/n) (#19010).

**Test**
Test modified in SharePartitionTest.java covering the corrected behaviour.
