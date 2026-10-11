# Bug 1186: thriftColumnNameType Always Returns UTF8 for Static Compact Tables, Ignoring Actual Clustering Type

**Commit**: 412e8743d7 · **Ticket**: CASSANDRA-9825
**Subsystem**: src/java/org/apache/cassandra/config/CFMetaData.java
**Category**: Wrong constant / default value

---

## Root Cause

`thriftColumnNameType()` fell through to returning `UTF8Type.instance` as a default for all non-super-column tables, including static compact tables whose clustering column has an explicit non-string comparator (e.g., `IntegerType`, `LongType`). Thrift validation uses this method to check that column names conform to the declared comparator; returning UTF-8 unconditionally meant that writes with binary integer names would be accepted and stored but silently misread, or that validation rejected valid non-UTF-8 names.

### Before / After

```java
// BEFORE
public AbstractType<?> thriftColumnNameType()
{
    if (isSuper()) { ... }
    // fell through to:
    return UTF8Type.instance;
}
```

```java
// AFTER
public AbstractType<?> thriftColumnNameType()
{
    if (isSuper()) { ... }
    assert isStaticCompactTable();
    return clusteringColumns.get(0).type;
}
```

---

## How to Spot

Default return values at the end of type-dispatch methods are a common source of silent misclassification. Whenever a method switches on a table or schema property and ends with a hardcoded fallback type, ask whether every possible structural variant is explicitly covered; an `assert` or `throw` is safer than returning a plausible-looking default that works for the common case but silently corrupts the uncommon one.

---

## Test

No test added.
