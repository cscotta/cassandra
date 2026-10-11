# Bug 1848: Fix Build — Wrong MarshalException Import and Wrong Range Tombstone Comparator

**Commit**: 7fb2d24fa3 · **Ticket**: n/a
**Subsystem**: tools/SSTableImport
**Category**: Wrong constant / default value

---

## Root Cause

Two errors introduced by prior work on `SSTableImport.java` broke compilation. First, `MarshalException` was imported from the old `db.marshal` package after it had been moved to `serializers`. Second, when serializing a range-tombstone end value the code constructed a new single-component `CompositeType` wrapper around the first column-definition comparator instead of simply using the existing `comparator` field, which already carries the correct full composite type for the table. Using a freshly constructed single-component type could produce an incorrect byte encoding for tables with more than one clustering component.

### Before / After

```java
// BEFORE
import org.apache.cassandra.db.marshal.MarshalException;
...
AbstractType<?> type = CompositeType.getInstance(Arrays.asList(new AbstractType<?>[]{meta.getColumnDefinitionComparator(0)}));
value = type.fromString((String)fields.get(1));
```

```java
// AFTER
import org.apache.cassandra.serializers.MarshalException;
...
value = comparator.fromString((String)fields.get(1));
```

---

## How to Spot

When a refactor moves a class to a new package, search for the old fully-qualified name across the whole tree before committing. For type-selection logic, prefer reusing an already-resolved type field over constructing a new type inline from raw metadata fragments — the two can silently diverge.

---

## Test

No test added.
