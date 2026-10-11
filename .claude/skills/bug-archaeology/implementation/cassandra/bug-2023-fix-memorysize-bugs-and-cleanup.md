# Bug 2023: Incorrect `memorySize()` Accounting in Cache Key and Index Classes

**Commit**: fe10ba7df3 · **Ticket**: CASSANDRA-5564
**Subsystem**: src/java/org/apache/cassandra/cache/KeyCacheKey.java, RowCacheKey.java, RowCacheSentinel.java, src/java/org/apache/cassandra/db/RowIndexEntry.java, src/java/org/apache/cassandra/io/sstable/IndexHelper.java, src/java/org/apache/cassandra/utils/ObjectSizes.java
**Category**: Wrong constant / default value

---

## Root Cause

Several `memorySize()` implementations incorrectly used `ObjectSizes.getSizeWithRef(byte[])`, which included a reference-size header that was double-counted when the caller also called `getFieldSize()` for the owning object's fields. Additionally, `RowIndexEntry`'s indexed subclass computed list overhead incorrectly, and `RowCacheSentinel` hardcoded `8` bytes for its `long` field instead of using `TypeSizes.NATIVE.sizeof()`. The fix separates reference-size accounting (tracked in the parent object's field size) from array-body accounting (tracked separately via `getArraySize(byte[])`), and introduces a dedicated `getArraySize(byte[])` overload to make the intent explicit.

### Before / After

```java
// BEFORE — reference size double-counted
long fields = ObjectSizes.getReferenceSize() + ObjectSizes.getSizeWithRef(key);
return ObjectSizes.getFieldSize(fields);
```

```java
// AFTER — reference counted in fields, array body counted separately
return ObjectSizes.getFieldSize(ObjectSizes.getReferenceSize() +  // desc ref
                                ObjectSizes.getReferenceSize())   // key ref
       + ObjectSizes.getArraySize(key);
```

```java
// BEFORE — hardcoded constant for long field size
return ObjectSizes.getFieldSize(8);
```

```java
// AFTER — uses TypeSizes for portability
return ObjectSizes.getFieldSize(TypeSizes.NATIVE.sizeof(sentinelId));
```

---

## How to Spot

In `memorySize()` implementations, trace every field: each reference contributes `getReferenceSize()` to the owning object's field block, and the pointed-to object is measured separately. Using `getSizeWithRef()` inside `getFieldSize()` double-counts the reference. A helper method named `getSizeWithRef` is a code smell in this pattern — prefer explicit separation of the reference and the referent.

---

## Test

No test added.
