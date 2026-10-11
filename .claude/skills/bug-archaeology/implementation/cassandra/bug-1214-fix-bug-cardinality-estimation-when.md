# Bug 1214: Cardinality Estimation Corrupted by In-Progress Compaction SSTables

**Commit**: 42ea6e1f37 · **Ticket**: CASSANDRA-9580
**Subsystem**: src/java/org/apache/cassandra/io/sstable/SSTableReader.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`SSTableReader.getApproximateKeyCount()` iterates over a set of SSTables and merges their HyperLogLog cardinality sketches to estimate the total number of distinct partition keys. SSTables opened with `OpenReason.EARLY` are partial, in-progress views written during compaction and share data with the full sstables that will eventually replace them. Including `EARLY` readers in the merge caused the same key data to be counted multiple times — once from the original sstable and again from the partially-written compaction output — inflating the cardinality estimate and leading to incorrect decisions downstream (e.g., bloom filter sizing).

### Before / After

```java
// BEFORE
for (SSTableReader sstable : sstables)
{
    try
    {
        CompactionMetadata metadata = ...deserialize...;
```

```java
// AFTER
for (SSTableReader sstable : sstables)
{
    if (sstable.openReason == OpenReason.EARLY)
        continue;

    try
    {
        CompactionMetadata metadata = ...deserialize...;
```

---

## How to Spot

Whenever code aggregates statistics across a collection of SSTables, verify that readers in transient or intermediate states (`EARLY`, shadow copies) are excluded. Any loop that processes "all sstables" for a metric that must not double-count data is suspect if it does not filter on `openReason`.

---

## Test

No test added.
