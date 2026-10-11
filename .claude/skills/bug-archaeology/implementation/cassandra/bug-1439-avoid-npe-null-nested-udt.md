# Bug 1439: Avoid NPE on Null Nested UDT Inside a Set

**Commit**: f6ea46e93c · **Ticket**: CASSANDRA-8105
**Subsystem**: src/java/org/apache/cassandra/db/marshal/TupleType.java
**Category**: Missing null / bounds check

---

## Root Cause

`TupleType.compare()` incremented its field index `i` at the bottom of the while loop body, then used `types.get(i)` at the top of the next iteration. When a nested UDT inside a collection (e.g., a set of frozen UDTs) had a null field, the byte-level parsing could produce a zero-remaining buffer segment before all type fields were exhausted, allowing `i` to go out of sync with the buffer position. Additionally, the separate `int i = 0` declaration and `++i` increment at the loop tail was error-prone: if any early `return` or `break` inside the loop was later added, the increment would be skipped silently. The fix converts the while loop to a `for` loop with `i` declared and incremented in the loop header, tying the counter's lifecycle to the loop itself.

### Before / After

```java
// BEFORE
int i = 0;
while (bb1.remaining() > 0 && bb2.remaining() > 0)
{
    AbstractType<?> comparator = types.get(i);
    // ... read value1, value2, compare ...
    ++i;  // increment at bottom — can be skipped if logic changes
}
```

```java
// AFTER
for (int i = 0; bb1.remaining() > 0 && bb2.remaining() > 0; i++)
{
    AbstractType<?> comparator = types.get(i);
    // ... read value1, value2, compare ...
    // no manual increment needed
}
```

---

## How to Spot

When a loop iterates in parallel over both a buffer (consuming bytes) and an index into a type list, any mismatch between how the buffer advances and how the index advances can cause an `IndexOutOfBoundsException` or a type mismatch. Refactoring such loops to `for` loops with the index in the header eliminates the risk of forgotten increments. Additionally, watch for UDT/tuple comparators that do not explicitly handle null fields — a null serialized as an empty or zero-length component can cause the buffer-exhaustion condition to trigger before all type slots are visited.

---

## Test

`UserTypesTest.testCassandra8105()` inserts rows with nested frozen UDTs where inner fields are omitted (null), covering sets, lists, and mixed nesting depths to confirm no NPE or comparison error occurs.
