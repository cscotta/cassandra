# Bug 0718: ViewBuilder restart skips all SSTables when generation filter excludes compacted files (trunk)

**Commit**: 2f1ab4a424 · **Ticket**: CASSANDRA-13405
**Subsystem**: Materialized views / ViewBuilder
**Category**: Incorrect filtering / wrong query result

---

## Root Cause
Same root cause as commit be96c2840b (CASSANDRA-13405 backport): when a materialized view build was interrupted, the stored maximum SSTable generation number was used on restart to filter the canonical SSTable set. Compactions occurring between the original start and the restart produce SSTables with higher generation numbers, which the filter excludes, leaving the resumed build with an empty or incomplete SSTable set and silently omitting rows from the view. This trunk commit carries the same fix — generation tracking is dropped (stored as `0`) and the canonical full SSTable set is used unconditionally — plus additional improvements: `View.getReadQuery()` gains trace logging, `View.stopBuild()` gains a debug log, and per-build key counts are logged at completion.

### Before / After
```java
// BEFORE — resume branch sets an SSTable filter
else
{
    function = view -> {
        Iterable<SSTableReader> readers = canonicalFunction.apply(view);
        if (readers != null)
            return Iterables.filter(readers,
                r -> r.descriptor.generation <= buildStatus.left);
        return null;
    };
    lastToken = buildStatus.right;
}

// AFTER — no filter; full canonical set used for both fresh and resumed builds
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
Token-based progress checkpointing is safe across compactions because tokens are stable; generation-based checkpointing is not because compaction replaces SSTables with new higher-generation files. Any ViewBuilder or index-builder resume path that filters SSTables by stored generation number should be reviewed: if a compaction can run between the checkpoint write and the next resume, the filtering will silently miss data.

---

## Test
`ViewTest` extended with a test that starts a view build, interrupts it, forces a compaction, and restarts the build, verifying the view contains all base-table rows.
