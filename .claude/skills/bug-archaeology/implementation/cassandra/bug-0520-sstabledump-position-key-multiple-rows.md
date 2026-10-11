# Bug 0520: sstabledump Position Key Wrong for Multi-Row Partitions

**Commit**: de4e2504f4 · **Ticket**: CASSANDRA-14721
**Subsystem**: src/java/org/apache/cassandra/tools/JsonTransformer.java
**Category**: Logic error in condition

---

## Root Cause

The `serializePartition` method in `JsonTransformer` only opened the `rows` JSON array when `partition.hasNext() || partition.staticRow() != null`. This guarded call meant that `updatePosition()` — which records the current file offset into the `position` field — was only invoked inside the block. For single-row partitions where the row was already consumed by the time the condition was evaluated, or in edge cases where a partition had rows but the condition evaluated incorrectly, `updatePosition()` was called at the wrong moment, producing a stale or incorrect position value. Additionally, the conditional structure double-checked `hasNext()`, which advances the iterator state, creating ordering issues.

The fix unconditionally opens the `rows` array and calls `updatePosition()` before iterating, ensuring the position always reflects the start of the rows section regardless of partition structure.

### Before / After

```java
// BEFORE
if (partition.hasNext() || partition.staticRow() != null)
{
    json.writeFieldName("rows");
    json.writeStartArray();
    updatePosition();
    if (!partition.staticRow().isEmpty())
        serializeRow(partition.staticRow());
    Unfiltered unfiltered;
    updatePosition();
    while (partition.hasNext()) { ... }
    json.writeEndArray();
    json.writeEndObject();
}
```

```java
// AFTER
json.writeFieldName("rows");
json.writeStartArray();
updatePosition();
if (partition.staticRow() != null)
{
    if (!partition.staticRow().isEmpty())
        serializeRow(partition.staticRow());
    updatePosition();
}
Unfiltered unfiltered;
while (partition.hasNext()) { ... }
json.writeEndArray();
json.writeEndObject();
```

---

## How to Spot

When JSON serialization has a guarded block that both opens a container (array/object) and records positional state, ask whether the guard can suppress the state-recording path even when data is present. If position tracking calls live inside a conditional that also controls structural JSON output, any false-negative from the condition silently produces a wrong position while still emitting partial output elsewhere.

---

## Test

No test added.
