# Bug 023: Fix IndexOutOfBoundsException in sstablemetadata tool when a range tombstone is a max clustering value

**Commit**: 1c5286f4d1 · **Ticket**: CASSANDRA-20855
**Subsystem**: Tools / SSTable metadata viewer
**Category**: Missing null / bounds check

---

## Root Cause

`SSTableMetadataViewer` printed `minClusteringValues` and `maxClusteringValues` by iterating `0..clusteringTypes.size()-1` and indexing into `stats.maxClusteringValues` at the same position. When the maximum clustering value stored in SSTable statistics comes from a range tombstone boundary rather than a regular row, only the columns up to the tombstone's bound are recorded — the list can be shorter than the number of clustering columns. The guard condition `header.getClusteringTypes().size() == stats.minClusteringValues.size()` was checked only for `minClusteringValues` and assumed `maxClusteringValues` had the same length, so a mismatch on the max side threw `IndexOutOfBoundsException`.

### Before / After

```java
// BEFORE — assumes min and max clustering value lists are always the same length
if (header != null && header.getClusteringTypes().size() == stats.minClusteringValues.size())
{
    List<AbstractType<?>> clusteringTypes = header.getClusteringTypes();
    // ...iterates over clusteringTypes.size() positions...
    for (int i = 0; i < clusteringTypes.size(); i++)
    {
        minValues[i] = clusteringTypes.get(i).getString(minClusteringValues.get(i));
        maxValues[i] = clusteringTypes.get(i).getString(maxClusteringValues.get(i)); // IOOBE here
    }
}

// AFTER — each list is printed independently using Math.min to cap the loop
private void printClusteringValues(SerializationHeader.Component header, String name, List<ByteBuffer> clusteringValues)
{
    if (header != null && header.getClusteringTypes().size() >= clusteringValues.size())
    {
        int size = Math.min(clusteringTypes.size(), clusteringValues.size());
        for (int i = 0; i < size; i++)
            values[i] = clusteringTypes.get(i).getString(clusteringValues.get(i));
        field(name, Arrays.toString(values));
    }
}
```

---

## How to Spot

When two parallel lists are assumed to have the same length and only one is validated, any code path that produces a shorter list for the other will throw `IndexOutOfBoundsException`. A reviewer should check that every list access inside the same loop is guarded against its own length, or that the loop bound is `Math.min` of all participating lists. Tombstone boundaries are a known source of partial clustering keys in Cassandra metadata, so tools that display clustering statistics should always tolerate shorter-than-schema-width clustering value lists.

---

## Test

`SSTableMetadataViewerTest.testLoadingOfSStableWithRangeTombstoneAsMaxClustering` loads a pre-built SSTable containing a range tombstone whose bound becomes the max clustering value, runs the viewer tool, and asserts that `maxClusteringValues` is printed with only the first clustering key and that the tool exits cleanly with no stderr output.
