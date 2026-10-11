# Bug 1179: Fix MarshalException When Upgrading Super Column Families

**Commit**: 172a9754d6 · **Ticket**: CASSANDRA-9582
**Subsystem**: src/java/org/apache/cassandra/config/ColumnDefinition.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

When deserializing a `ColumnDefinition` for a REGULAR column in a super column family, the code read the `COMPONENT_INDEX` from the stored schema row. For super columns the component index must always be `1` (pointing at the column component), but a previously persisted or migrated row could contain an incorrect value in the `COMPONENT_INDEX` field due to the historical bug tracked in CASSANDRA-9582. Trusting that stored value caused a `MarshalException` at read time because the comparator was invoked with the wrong index. The fix hard-codes `componentIndex = 1` for the super+REGULAR case before consulting the stored field, ignoring any corrupted on-disk value.

### Before / After

```java
// BEFORE
Integer componentIndex = null;
if (row.has(COMPONENT_INDEX))
    componentIndex = row.getInt(COMPONENT_INDEX);
else if (kind == Kind.CLUSTERING_COLUMN && isSuper)
    componentIndex = 1;
```

```java
// AFTER
Integer componentIndex = null;
if (kind == Kind.REGULAR && isSuper)
    componentIndex = 1; // don't trust COMPONENT_INDEX due to #9582
else if (row.has(COMPONENT_INDEX))
    componentIndex = row.getInt(COMPONENT_INDEX);
```

---

## How to Spot

Deserialization code that reads a stored index or type hint without validating it against a known invariant for a specific schema variant (super column, compact storage, etc.) is vulnerable to silent corruption propagated from older or buggy writers. Reviewers should check whether any special-cased table type has fixed structural constraints that override what is stored on disk, and ensure those constraints are applied before reading the persisted value.

---

## Test

No dedicated test added for this specific regression.
