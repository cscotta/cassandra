# Bug 1662: ColumnIdentifier Uses Signed ByteBuffer Comparison Instead of Unsigned

**Commit**: 8cb81f74a8 · **Ticket**: CASSANDRA-7089
**Subsystem**: src/java/org/apache/cassandra/cql3, src/java/org/apache/cassandra/db
**Category**: Logic error in condition

---

## Root Cause

`ColumnIdentifier` implemented `Comparable<ColumnIdentifier>` using `ByteBuffer.compareTo()`, which performs a *signed* byte-by-byte comparison. Cassandra's column ordering throughout the storage engine is defined as *unsigned* byte comparison. Any `TreeMap` or `SortedSet` keyed on `ColumnIdentifier` would therefore produce incorrect ordering for column names containing bytes with the high bit set. Separately, `Composite.isPrefixOf()` used `ByteBuffer.equals()` to compare composite components instead of delegating to the type-aware `CType.subtype(i).compare()`, causing prefix checks to fail whenever the type's equality semantics differed from raw byte equality (e.g., with counter or reversed types). The fix removes `Comparable` from `ColumnIdentifier`, switches the `CFMetaData` column comparator to `ByteBufferUtil.compareUnsigned`, and threads `CType` through the `isPrefixOf` / `maySelectPrefix` call chain.

### Before / After

```java
// BEFORE – ColumnIdentifier (signed comparison)
public int compareTo(ColumnIdentifier other) {
    return bytes.compareTo(other.bytes); // signed!
}
```

```java
// AFTER – CFMetaData comparator (unsigned)
return ByteBufferUtil.compareUnsigned(def1.name.bytes, def2.name.bytes);
```

```java
// BEFORE – AbstractComposite.isPrefixOf (raw equals)
if (!get(i).equals(c.get(i))) return false;
```

```java
// AFTER – type-aware comparison
if (type.subtype(i).compare(get(i), c.get(i)) != 0) return false;
```

---

## How to Spot

Whenever `ByteBuffer` values represent encoded Cassandra data, use `ByteBufferUtil.compareUnsigned` rather than `ByteBuffer.compareTo`. Review every `Comparable` implementation on classes that wrap `ByteBuffer` to confirm it uses unsigned semantics. Likewise, composite prefix checks must use type comparators, not raw `equals`.

---

## Test

No test added.
