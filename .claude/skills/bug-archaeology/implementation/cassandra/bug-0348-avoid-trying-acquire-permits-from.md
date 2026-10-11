# Bug 0348: Rate Limiter Throws Exception When Acquiring 0 Permits During Snapshot

**Commit**: b9d8700355 · **Ticket**: CASSANDRA-16872
**Subsystem**: src/java/org/apache/cassandra/io/sstable/format/SSTableReader.java
**Category**: Logic error in condition

---

## Root Cause

When taking a snapshot, `ColumnFamilyStore` called `rateLimiter.acquire(SSTableReader.componentsFor(ssTable.descriptor).size())` before creating hard links. If an SSTable's TOC file was empty or corrupted, `componentsFor` returned an empty set, resulting in a call to `rateLimiter.acquire(0)`. Guava's `RateLimiter.acquire(0)` throws an `IllegalArgumentException` because acquiring zero permits is not a valid operation. The fix moved the rate-limiter acquisition inside the per-component loop in `SSTableReader.createLinks`, where it acquires exactly 1 permit per component file that actually exists, thus naturally skipping missing or zero components.

### Before / After

```java
// BEFORE (ColumnFamilyStore)
rateLimiter.acquire(SSTableReader.componentsFor(ssTable.descriptor).size());
ssTable.createLinks(snapshotDirectory.getPath());
```

```java
// AFTER (SSTableReader.createLinks loop)
for (Component component : components) {
    File sourceFile = new File(descriptor.filenameFor(component));
    if (!sourceFile.exists())
        continue;
    if (null != limiter)
        limiter.acquire();   // acquire 1 per existing file
    FileUtils.createHardLink(sourceFile, targetLink);
}
```

---

## How to Spot

Any call to `RateLimiter.acquire(n)` where `n` is derived from a collection size should guard against `n == 0`. More robustly, move the acquire to the innermost loop so permits are only consumed for work that actually happens, which also avoids over-acquiring when files are missing.

---

## Test

Adds `SnapshotTest.testEmptyTOC`: truncates the TOC file to zero bytes for a live SSTable and then calls `snapshot()`, asserting it completes without throwing.
