# Bug 0829: JsonTransformer missing endObject for partition-level deletion

**Commit**: fa14804543 · **Ticket**: CASSANDRA-12418
**Subsystem**: tools / JsonTransformer (sstabledump / nodetool)
**Category**: Logic error in condition

---

## Root Cause
`JsonTransformer.serializePartition()` handled partitions with a partition-level deletion by calling `serializeDeletion()`, which writes a JSON object for the deletion info, but then fell through without calling `json.writeEndObject()`. This left the Jackson `JsonGenerator` in an open-object state: all subsequent output was written as additional fields of that unclosed object instead of as sibling elements, producing malformed JSON that could not be parsed by any consumer.

### Before / After
```java
// BEFORE — missing closing brace for partition-deletion object
if (!partition.partitionLevelDeletion().isLive())
{
    serializeDeletion(partition.partitionLevelDeletion());
    // writeEndObject() missing
}

// AFTER
if (!partition.partitionLevelDeletion().isLive())
{
    serializeDeletion(partition.partitionLevelDeletion());
    json.writeEndObject();   // closes the deletion object
}
```

---

## How to Spot
When a streaming JSON writer opens an object in a helper method and the caller is responsible for closing it, any conditional branch that calls the helper but lacks the matching `writeEndObject()` call will corrupt the output. Review every call site of `serializeDeletion` (and similar helpers) to confirm each open is matched by a close. Integration tests that parse the complete JSON output of `sstabledump` against tables containing deleted partitions would catch this immediately.

---

## Test
No new test added.
