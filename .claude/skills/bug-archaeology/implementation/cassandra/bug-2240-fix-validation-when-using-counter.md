# Bug 2240: Wrong Comparator Used to Serialize Column Names in Counter Validation

**Commit**: 0dda10d765 · **Ticket**: CASSANDRA-4706
**Subsystem**: src/java/org/apache/cassandra/config/CFMetaData.java
**Category**: Wrong constant / default value

---

## Root Cause

When validating that a column family's columns are consistently all-counter or all-non-counter, the error message serialized the column name using the table-level `comparator` directly. For CQL3 tables backed by a `CompositeType` comparator, this produced an unreadable composite binary blob instead of the human-friendly column name. The fix calls `getColumnDefinitionComparator(def)`, which returns the per-column comparator component that correctly decodes the individual column name segment.

### Before / After

```java
// BEFORE
throw new ConfigurationException("Cannot add a non counter column (" + comparator.getString(def.name) + ") ...");
// ...
throw new ConfigurationException("Cannot add a counter column (" + comparator.getString(def.name) + ") ...");
```

```java
// AFTER
throw new ConfigurationException("Cannot add a non counter column (" + getColumnDefinitionComparator(def).getString(def.name) + ") ...");
// ...
throw new ConfigurationException("Cannot add a counter column (" + getColumnDefinitionComparator(def).getString(def.name) + ") ...");
```

---

## How to Spot

Any time code calls `comparator.getString(columnDef.name)` on a table that may use `CompositeType`, the result will be garbled for CQL3 tables. The correct pattern is to obtain the column's specific component comparator via a helper like `getColumnDefinitionComparator(def)`.

---

## Test

No test added.
