# Bug 1460: Potential Exception with ReversedType in DynamicCompositeType

**Commit**: b93ca58d4f · **Ticket**: CASSANDRA-7898
**Subsystem**: src/java/org/apache/cassandra/db/marshal/DynamicCompositeType.java
**Category**: Logic error in condition

---

## Root Cause

`DynamicCompositeType.compareNextComponent` extracts the comparator from each buffer and, when the two comparators differ, calls `comp1.compare(bb1, bb2)` to establish ordering between them. When both comparators are `ReversedType` wrappers the method unwrapped them to their base types for the comparison (correct for determining whether the types are compatible), but then returned the unwrapped `comp1` as the comparator to use for the actual value comparison. This means the reversal was silently discarded: the values were compared in ascending order even though the column was declared with a descending type, producing wrong sort order and potential `ClassCastException` when `ReversedType<A>` and `ReversedType<B>` have incompatible base types.

### Before / After

```java
// BEFORE
AbstractType<?> comp1 = getComparator(bb1);
AbstractType<?> comp2 = getComparator(bb2);
// ... unwrap both when both are ReversedType ...
if (comp1 != comp2) { /* type compatibility check */ }
return comp1;  // returns unwrapped type — reversal lost
```

```java
// AFTER
AbstractType<?> comp1 = getComparator(bb1);
AbstractType<?> comp2 = getComparator(bb2);
AbstractType<?> rawComp = comp1;  // save original before potential unwrap
if ((comp1 instanceof ReversedType) && (comp2 instanceof ReversedType)) {
    comp1 = ((ReversedType<?>) comp1).baseType;
    comp2 = ((ReversedType<?>) comp2).baseType;
}
if (comp1 != comp2) { /* type compatibility check on base types */ }
return rawComp;  // return the original, reversal-preserving comparator
```

---

## How to Spot

When a method temporarily unwraps a type wrapper for a narrower sub-operation (e.g. compatibility checking), care is required to return the original wrapped value rather than the unwrapped version. A reviewer should check every code path that rebinds a local variable that was intended to carry type information, and ensure the return value reflects the full type including any wrappers.

---

## Test

Two new test methods were added to `DynamicCompositeTypeTest`: `testFullRoundReversed` exercises a full write-and-read round-trip for a table using reversed components and checks the physical sort order, and `testUncomparableReversedColumns` verifies that comparing two columns whose reversed base types are incompatible does not throw an exception (instead it produces the correct ordering constant).
