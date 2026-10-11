# Bug 1622: Fixup for CASSANDRA-6914: Null Handling and Wrong Target Node

**Commit**: 9d06ea6f14 · **Ticket**: CASSANDRA-7155
**Subsystem**: src/java/org/apache/cassandra/cql3/ColumnCondition.java, cql3/statements/
**Category**: Missing null / bounds check

---

## Root Cause

The CAS (compare-and-set / lightweight transaction) condition evaluation code introduced in CASSANDRA-6914 had multiple null-handling defects. The `ColumnCondition.WithVariables` inner class did not guard against null values when comparing collection element accesses, causing a `NullPointerException` when an `IF map[key] = null` or `IF list[idx] = null` condition was evaluated. Additionally, `ModificationStatement` built the column name list used to select the read-before-write target row using a `List`, which allowed duplicate entries when multiple conditions referenced the same collection column; the fix switched to `LinkedHashSet` to deduplicate while preserving order.

### Before / After

```java
// BEFORE - no null guard on collection element access condition;
// list allowed duplicate column names causing wrong target-row fetch
private WithVariables(List<ByteBuffer> variables) { ... }

public boolean equalsTo(WithVariables other) throws InvalidRequestException
{
    // ... no null-check before comparing values
    return value().bindAndGet(variables).equals(other.value().bindAndGet(other.variables));
}

List<CFDefinition.Name> names = new ArrayList<CFDefinition.Name>();
```

```java
// AFTER - null is handled explicitly; duplicates eliminated
if (collectionElement == null)
    throw new InvalidRequestException(
        "Invalid null value for " + (column.type instanceof MapType ? "map" : "list") + " element access");

if (value == null)
    return c == null || !c.isLive(now);   // null means "column must not exist"

Set<CFDefinition.Name> names = new LinkedHashSet<CFDefinition.Name>();
```

---

## How to Spot

Whenever user-supplied values from a bind-variable list are used for comparison without being checked for null first, and especially when dealing with collection element access (map/list indexing), add an explicit null guard and define the expected semantics (reject null, treat as "not present", etc.). Also review any place that accumulates column names for a read operation using a List — duplicates cause the read to target the wrong row.

---

## Test

A new `CollectionTypeTest` covering collection condition evaluation was added.
