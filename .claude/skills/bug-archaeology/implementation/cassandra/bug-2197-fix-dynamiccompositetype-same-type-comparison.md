# Bug 2197: DynamicCompositeType Always Returns "Lesser" When Comparing Columns of Different Types

**Commit**: 5e15927fff · **Ticket**: CASSANDRA-4711
**Subsystem**: src/java/org/apache/cassandra/db/marshal/DynamicCompositeType.java
**Category**: Wrong constant / default value

---

## Root Cause

`DynamicCompositeType.getComparatorFor()` returns a `FixedValueComparator` when two composite components have different concrete types, indicating that all values of the left type should sort before (or after) all values of the right type. The old implementation had only a single `FixedValueComparator.instance` whose `compare()` always returned `-1` (less than). When the class-name comparison yielded `cmp > 0` the code returned `ReversedType.getInstance(FixedValueComparator.instance)` expecting the reversal wrapper to negate the result, but this reversed the operands rather than negating the fixed return value, producing incorrect ordering. The fix replaces the single instance with two distinct singletons—`alwaysLesserThan` (returns `-1`) and `alwaysGreaterThan` (returns `+1`)—and selects between them directly based on the class-name comparison sign.

### Before / After

```java
// BEFORE – single instance always returns -1; "greater" case mis-handled via ReversedType
public static final FixedValueComparator instance = new FixedValueComparator();
public int compare(ByteBuffer v1, ByteBuffer v2) { return -1; }
// ...
return cmp < 0 ? FixedValueComparator.instance
               : ReversedType.getInstance(FixedValueComparator.instance);
```

```java
// AFTER – separate singletons for each ordering direction
public static final FixedValueComparator alwaysLesserThan  = new FixedValueComparator(-1);
public static final FixedValueComparator alwaysGreaterThan = new FixedValueComparator(+1);
// ...
return cmp < 0 ? FixedValueComparator.alwaysLesserThan
               : FixedValueComparator.alwaysGreaterThan;
```

---

## How to Spot

When a comparator is used bidirectionally (both "A < B" and "A > B" outcomes needed), a single instance with a hard-coded return value cannot cover both cases. Review any comparator helper whose `compare()` returns a literal constant to confirm that both the positive and negative directions are handled by distinct objects or a runtime parameter, not by wrapping with `ReversedType`.

---

## Test

No test added.
