# Bug 1732: CQLSSTableWriter.addRow(Map) Crashes on Missing Columns

**Commit**: ba95ca0db5 · **Ticket**: CASSANDRA-6526
**Subsystem**: src/java/org/apache/cassandra/io/sstable/CQLSSTableWriter.java
**Category**: Missing null / bounds check

---

## Root Cause

`CQLSSTableWriter.addRow(Map<String, Object>)` iterated only up to `Math.min(values.size(), boundNames.size())` entries, meaning it silently skipped bound parameters when the caller supplied fewer map entries than expected columns. Worse, when a map key matched a column name that had a `null` value, `AbstractType.decompose(null)` would throw a `NullPointerException`. The fix iterates over all `boundNames` (the authoritative list), performs a map lookup for each, and treats a missing or `null` map entry as an explicit `null` buffer, which the downstream writer correctly handles as an absent column.

### Before / After

```java
// BEFORE
int size = Math.min(values.size(), boundNames.size());
List<ByteBuffer> rawValues = new ArrayList<>(size);
for (int i = 0; i < size; i++) {
    ColumnSpecification spec = boundNames.get(i);
    rawValues.add(((AbstractType)spec.type).decompose(values.get(spec.name.toString())));
}
```

```java
// AFTER
int size = boundNames.size();
List<ByteBuffer> rawValues = new ArrayList<>(size);
for (int i = 0; i < size; i++) {
    ColumnSpecification spec = boundNames.get(i);
    Object value = values.get(spec.name.toString());
    rawValues.add(value == null ? null : ((AbstractType)spec.type).decompose(value));
}
```

---

## How to Spot

When a method accepts a `Map` as a flexible alternative to a positional list, always iterate over the canonical schema/bound-names list rather than the map itself. Using `Math.min` between an input collection size and a schema size almost always silently drops columns. Separately, any call to `decompose(userInput)` needs a null guard because map lookups return null for absent keys.

---

## Test

A new test case calls `addRow(ImmutableMap.of("k", 3, "v2", 12))` (omitting `v1`) and asserts that the resulting row has `k=3`, `v1=null`, and `v2=12`, confirming both missing-column tolerance and correct round-trip for supplied columns.
