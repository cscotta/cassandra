# Bug 0918: sstabledump silently drops all cells after a tombstone marker

**Commit**: 049d70c2e6 · **Ticket**: CASSANDRA-11654
**Subsystem**: Tools / sstabledump
**Category**: Logic error in condition

---

## Root Cause
`JsonTransformer.serializeRow()` used a mutually exclusive `if / else` structure when writing row JSON: it would either write a deletion object (when the row had a primary-key liveness tombstone / row deletion) **or** write the `cells` array — never both. In practice a row can carry both a row-level tombstone marker and live (or separately deleted) cells. The `else` branch was simply never reached for such rows, so `sstabledump` silently omitted every cell after the first tombstone marker, producing incomplete and misleading JSON output.

### Before / After
```java
// BEFORE
if (row.deletion().isLive(liveInfo)) {
    // ... write deletion object ...
    json.writeEndObject();
} else {
    json.writeFieldName("cells");
    json.writeStartArray();
    row.cells().forEach(c -> serializeCell(c, liveInfo));
    json.writeEndArray();
}

// AFTER – always write cells, regardless of row deletion
if (!row.deletion().isLive(liveInfo)) {
    // ... write deletion object ...
    json.writeEndObject();
    objectIndenter.setCompact(false);
}
json.writeFieldName("cells");
json.writeStartArray();
row.cells().forEach(c -> serializeCell(c, liveInfo));
json.writeEndArray();
json.writeEndObject();
```

---

## How to Spot
In any serialization/dumping code that branches on a "has deletion" condition, verify that the non-deletion data (cells, columns) is written in both branches or unconditionally. A mutual-exclusion `if/else` between metadata and payload is almost always wrong for Cassandra rows, which can simultaneously carry deletion markers and live cell data.

---

## Test
No new test added in this commit.
