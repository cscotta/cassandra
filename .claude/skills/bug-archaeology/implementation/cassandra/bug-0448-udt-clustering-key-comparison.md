# Bug 0448: UDT Clustering Key Comparison Fails After Adding New Field

**Commit**: 2f0eb6f799 · **Ticket**: CASSANDRA-15938
**Subsystem**: `src/java/org/apache/cassandra/db/marshal/TupleType.java`
**Category**: Logic error in condition

---

## Root Cause

`TupleType.compare()` decoded components from two byte-buffers using the loop condition `bb1.remaining() > 0 && bb2.remaining() > 0`, but never bounded `i` against `types.size()`. When a UDT field is added after data is written, rows serialized with the old schema have fewer components than the current `types` list, so `i` can exceed `types.size() - 1` causing an `IndexOutOfBoundsException`.

Additionally, the post-loop logic treated any remaining bytes in one buffer as a definitive ordering signal. After a UDT field addition, a row serialized without the new field and one with the field set to `null` are logically equal but had different buffer lengths, making the old code sort them as unequal. The fix adds a bounds check and a helper that inspects remaining components: if all of them have a size marker of `-1` (null), the two values are considered equal.

### Before / After

```java
// BEFORE
for (int i = 0; bb1.remaining() > 0 && bb2.remaining() > 0; i++)
{
    AbstractType<?> comparator = types.get(i); // IOOBE when i >= types.size()
    ...
}
if (bb1.remaining() == 0)
    return bb2.remaining() == 0 ? 0 : -1;
return 1;
```

```java
// AFTER
for (int i = 0; bb1.remaining() > 0 && bb2.remaining() > 0 && i < types.size(); i++)
{
    AbstractType<?> comparator = types.get(i);
    ...
}
if (bb1.remaining() == 0 && bb2.remaining() == 0)
    return 0;
if (bb1.remaining() == 0)
    return allRemainingComponentsAreNull(bb2) ? 0 : -1;
return allRemainingComponentsAreNull(bb1) ? 0 : 1;
```

---

## How to Spot

Whenever a `for` loop indexes into a collection using a counter that is incremented in tandem with consuming bytes from an external buffer, verify that the counter is also bounded by the collection's size. Mismatches arise naturally in schema-evolution scenarios where the serialized data may have fewer fields than the current in-memory type descriptor; any comparison or deserialization routine that relies solely on buffer exhaustion to stop iteration will misbehave with such mixed-generation data.

---

## Test

`TuplesWithNullsComparisonTest.testAddUDTField` inserts rows before and after an `ALTER TYPE ADD` operation, then asserts that point-lookup by the old clustering key form still returns exactly one row after flush and compaction.
