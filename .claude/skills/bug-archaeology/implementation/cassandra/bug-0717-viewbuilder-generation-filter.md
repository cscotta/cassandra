# Bug 0717: ViewBuilder restart skips all SSTables when generation filter excludes compacted files

**Commit**: be96c2840b · **Ticket**: CASSANDRA-13405
**Subsystem**: Materialized views / ViewBuilder
**Category**: Incorrect filtering / wrong query result

---

## Root Cause
When a materialized view build was interrupted and later resumed, `ViewBuilder` stored the maximum SSTable generation number seen at the start of the original build. On resumption it filtered the canonical SSTable set to include only SSTables with a generation number at or below that stored value. Because compactions between the two runs produce new SSTables with higher generation numbers, the resumed build's SSTable set could be completely empty, silently leaving rows uncopied into the view. The fix abandons generation-based filtering entirely: the generation field stored in `system.built_views` is now always written as `0`, the resume path uses the full canonical SSTable set (same as a fresh build), and the flush is moved outside both branches so it always runs before the SSTable snapshot is taken.

### Before / After
```java
// BEFORE — initial build records max generation; resume filters to <= that generation
if (buildStatus == null)
{
    baseCfs.forceBlockingFlush();
    int generation = Integer.MIN_VALUE;
    try (Refs<SSTableReader> temp = baseCfs.selectAndReference(function).refs)
    {
        for (SSTableReader reader : temp)
            generation = Math.max(reader.descriptor.generation, generation);
    }
    SystemKeyspace.beginViewBuild(ksname, viewName, generation);
    lastToken = null;
}
else
{
    function = view -> Iterables.filter(
        canonicalFunction.apply(view),
        r -> r.descriptor.generation <= buildStatus.left);
    lastToken = buildStatus.right;
}

// AFTER — generation stored as 0; no SSTable filtering; flush always runs first
if (buildStatus == null)
{
    lastToken = null;
    SystemKeyspace.beginViewBuild(ksname, viewName, 0);
}
else
{
    lastToken = buildStatus.right;
}
baseCfs.forceBlockingFlush();
function = org.apache.cassandra.db.lifecycle.View.selectFunction(SSTableSet.CANONICAL);
```

---

## How to Spot
Any checkpoint-and-resume pattern that uses a generation or sequence number to bound the work on resumption must account for the fact that compactions can replace old SSTables with new ones that have higher generation numbers. If the bound is derived from a snapshot taken before the work started and compactions run during the build, the resumed set may be empty or incomplete. Prefer token-based progress tracking (resume from last processed key) over generation-based SSTable filtering.

---

## Test
`ViewTest` extended with a test that starts a view build, interrupts it, forces a compaction (raising SSTable generation numbers), then restarts the build and verifies the view contains all base-table rows.
