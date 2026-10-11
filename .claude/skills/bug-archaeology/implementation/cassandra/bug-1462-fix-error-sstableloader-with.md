# Bug 1462: SSTableLoader Error with -cph > 1

**Commit**: ef65d58213 · **Ticket**: CASSANDRA-8007
**Subsystem**: src/java/org/apache/cassandra/streaming/StreamCoordinator.java
**Category**: State not cleaned up

---

## Root Cause

`StreamCoordinator.makeAndAddSections` distributed SSTables across sessions by slicing a list using a step size derived from the connections-per-host (`-cph`) setting. When `-cph > 1` the method iterated over `sstableDetails` multiple times (once per connection slot), assigning the same SSTable sections to multiple sessions. The list was never mutated between passes, so every pass processed the full collection again. The fix converts the iteration to use an explicit `Iterator` and calls `iter.remove()` after each element is consumed, ensuring each SSTable section is assigned to exactly one session regardless of how many connections are configured.

### Before / After

```java
// BEFORE
for (StreamSession.SSTableStreamingSections streamSession : sstableDetails)
{
    if (index % step == 0) { slice = new ArrayList<>(); result.add(slice); }
    slice.add(streamSession);
    ++index;
}
```

```java
// AFTER
Iterator<StreamSession.SSTableStreamingSections> iter = sstableDetails.iterator();
while (iter.hasNext())
{
    StreamSession.SSTableStreamingSections streamSession = iter.next();
    if (index % step == 0) { slice = new ArrayList<>(); result.add(slice); }
    slice.add(streamSession);
    ++index;
    iter.remove();  // consume each entry so it is not re-assigned
}
```

---

## How to Spot

When a collection is partitioned into multiple output buckets inside a loop, check whether the source collection is being consumed (entries removed or tracked) or only read. If the same collection is iterated from the beginning for every bucket, elements will be duplicated across buckets. The tell-tale sign is a `for-each` loop over a shared input that produces multiple output lists without any slicing or index tracking that prevents re-visiting earlier elements.

---

## Test

No test added.
