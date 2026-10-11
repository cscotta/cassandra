# Bug 1265: Fix Merge Issue with ColumnInfo Name Change

**Commit**: 8f4c43db02 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/hadoop/pig/CqlNativeStorage.java
**Category**: Wrong constant / default value

---

## Root Cause

A rename refactoring changed `ColumnInfo.getName()` from returning a `byte[]` to returning a `String`, but the Pig integration layer was not updated consistently. Two call sites still wrapped the result in `new String(...)` (treating it as raw bytes) and passed it to `validators.get(...)` as a raw `byte[]` via `ByteBufferUtil`-incompatible lookup, so the validator map lookup always missed and the schema field type defaulted to an incorrect value. The merge omitted the necessary adaptation of these two sites, leaving the Hadoop/Pig path silently broken.

### Before / After

```java
// BEFORE
AbstractType validator = validators.get(cdef.getName());
valSchema.setName(new String(cdef.getName()));
```

```java
// AFTER
AbstractType<?> validator = validators.get(ByteBufferUtil.bytes(cdef.getName()));
valSchema.setName(cdef.getName());
```

---

## How to Spot

After any rename refactoring that changes the type returned by a widely-used accessor, search for all call sites that wrap that accessor in a conversion constructor (like `new String(...)`) — these are almost certainly leftover from the old type and will silently produce wrong results or failed lookups.

---

## Test

No test added.
