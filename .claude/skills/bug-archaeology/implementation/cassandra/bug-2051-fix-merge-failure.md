# Bug 2051: ColumnIndex.Builder Constructor Signature Corrupted by Bad Merge

**Commit**: eba27a6413 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/db/ColumnIndex.java
**Category**: Logic error in condition

---

## Root Cause

A bad merge left the `ColumnIndex.Builder` constructor with a malformed parameter list: the closing parenthesis of the `DataOutput output` parameter was placed on the wrong line, so the new `boolean fromStream` parameter that was introduced in the same merge window was never actually appended. The constructor therefore lacked the `fromStream` parameter needed for the streaming path to disable the range tombstone tracker.

### Before / After

```java
// BEFORE (corrupted merge)
public Builder(ColumnFamily cf,
               ByteBuffer key,
               DataOutput output)
               DataOutput output,
               boolean fromStream)
```

```java
// AFTER
public Builder(ColumnFamily cf,
               ByteBuffer key,
               DataOutput output,
               boolean fromStream)
```

---

## How to Spot

Malformed constructor signatures like this will produce a compile error, but only if the file is actually compiled. Review merge results in files that were modified in multiple branches simultaneously, and always run a full compilation step before accepting a merge commit.

---

## Test

No test added.
