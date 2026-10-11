# Bug 0189: Empty SAI Column Index Fails Validation After Full-SSTable Streaming

**Commit**: e8fb4b2f63 · **Ticket**: CASSANDRA-19017
**Subsystem**: index/sai/disk/v1
**Category**: Missing null / bounds check

---

## Root Cause

When a column index was written with no indexed rows (empty index), the code wrote only a plain zero-byte completion marker file and skipped writing all other per-column index components (e.g., the trie, postings files). During post-streaming validation, `validatePerColumnIndexComponents` iterated over all expected per-column components and attempted to open each one, including the missing data files. Because the completion marker carried no information about emptiness, the validator had no way to know it should skip data components, causing `IOException` on the missing files and marking the index as corrupt.

### Before / After

```java
// BEFORE (writing)
indexDescriptor.createComponentOnDisk(IndexComponent.COLUMN_COMPLETION_MARKER, indexContext);
// plain empty file; no flag indicating emptiness

// BEFORE (validation)
for (IndexComponent indexComponent : perColumnIndexComponents(indexContext))
{
    if (isNotBuildCompletionMarker(indexComponent))
        // tries to open all components — fails if index is empty
        validateIndexComponent(indexDescriptor, indexContext, indexComponent, checksum);
}
```

```java
// AFTER (writing)
ColumnCompletionMarkerUtil.create(indexDescriptor, indexContext, true);  // isEmpty=true

// AFTER (validation)
isEmptyIndex = ColumnCompletionMarkerUtil.isEmptyIndex(indexDescriptor, indexContext);
// ...
if (!isEmptyIndex && isNotBuildCompletionMarker(indexComponent))
    validateIndexComponent(indexDescriptor, indexContext, indexComponent, checksum);
```

---

## How to Spot

When an index or data structure can legitimately have zero entries and therefore writes fewer files than the non-empty case, validation logic must distinguish the empty case before iterating over expected files. A completion marker that is a plain empty file cannot communicate state; it should encode at minimum an emptiness flag that the reader can inspect before attempting to open sibling components.

---

## Test

`IndexStreamingTest` streams SSTables with SAI indexes between two nodes using zero-copy streaming and asserts that all expected component files are transferred and that the receiving node passes validation without errors.
