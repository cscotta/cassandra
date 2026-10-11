# Bug 1312: Fix Rare NPE in ColumnDefinition#hasIndexOption()

**Commit**: 2206ffadd3 · **Ticket**: CASSANDRA-8786
**Subsystem**: src/java/org/apache/cassandra/config/ColumnDefinition.java
**Category**: Missing null / bounds check

---

## Root Cause

`ColumnDefinition#hasIndexOption()` called `indexOptions.containsKey(name)` without first checking whether `indexOptions` itself was null. For column definitions that have no index options configured, `indexOptions` can be null rather than an empty map, so any call to `hasIndexOption()` on such a column would throw a NullPointerException. The bug reflects a faulty assumption that the field is always initialized to a non-null collection.

### Before / After

```java
// BEFORE
public boolean hasIndexOption(String name)
{
    return indexOptions.containsKey(name);
}
```

```java
// AFTER
public boolean hasIndexOption(String name)
{
    return indexOptions != null && indexOptions.containsKey(name);
}
```

---

## How to Spot

When a field that backs a collection query method can be null (rather than an empty collection), every method that dereferences it without a null guard is a latent NPE. Reviewers should check whether the field has a guaranteed non-null initializer in all constructors and deserialization paths; if any path can leave it null, every use site needs a null check or the field should be defaulted to an empty collection at declaration time.

---

## Test

No test added.
