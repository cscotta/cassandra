# Bug 0752: sstabledump drops live rows when partition-level deletion exists

**Commit**: 883c9f0f74 · **Ticket**: CASSANDRA-13177
**Subsystem**: Tools / sstabledump
**Category**: Logic error in condition

---

## Root Cause
`JsonTransformer.serializePartition()` used a mutually exclusive `if/else` to decide whether to emit a partition-level deletion or the row array. When a partition had both a partition-level deletion *and* live rows, only the deletion marker was written and the `rows` array (including any static row and all regular rows) was silently skipped. The fix separates the two concerns: `serializeDeletion` is called whenever the deletion is live, and the `rows` array is opened whenever there are rows or a static row, independently.

### Before / After
```java
// BEFORE — rows silently dropped when partition deletion exists
if (!partition.partitionLevelDeletion().isLive())
{
    serializeDeletion(partition.partitionLevelDeletion());
    json.writeEndObject();
}
else
{
    json.writeEndObject();
    json.writeFieldName("rows");
    ...
}

// AFTER — deletion and rows written independently
if (!partition.partitionLevelDeletion().isLive())
    serializeDeletion(partition.partitionLevelDeletion());

json.writeEndObject();

if (partition.hasNext() || partition.staticRow() != null)
{
    json.writeFieldName("rows");
    ...
    json.writeEndObject();
}
```

---

## How to Spot
Watch for early `return`/`writeEndObject` branches inside serialisation methods that are gated on one boolean condition when in reality two orthogonal conditions must each be checked. Any time a single `if/else` decides whether to emit *mutually exclusive* outputs for what should be *independent* fields, that is a red flag — especially when one branch omits flushing the object close tag.

---

## Test
No new test added; the fix was verified by manually running `sstabledump` on an SSTable containing a partition-level deletion with live rows.
