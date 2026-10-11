# Bug 0359: Wrong Argument Order in LegacyLayout Error Message

**Commit**: ad139da84c · **Ticket**: CASSANDRA-15136
**Subsystem**: src/java/org/apache/cassandra/db (LegacyLayout)
**Category**: Wrong constant / default value

---

## Root Cause

`LegacyLayout` threw an `IllegalStateException` when it encountered a cell for an unknown column while reading a legacy SSTable.  The format string contained three `%s` placeholders in the order `(ksName, cfName, columnName)`, but the arguments were passed as `(metadata.ksName, metadata.cfName, UTF8Type.instance.compose(e.columnName))`, which put `ksName` where the column name should appear and the column name where `ksName` should appear.  The resulting error message displayed the keyspace name as the unknown column and the column name as the keyspace, making diagnosis significantly harder.

### Before / After

```java
// BEFORE
throw new IllegalStateException(String.format(
    "Got cell for unknown column %s in sstable of %s.%s: ...",
    metadata.ksName, metadata.cfName, UTF8Type.instance.compose(e.columnName)), e);
```

```java
// AFTER
throw new IllegalStateException(String.format(
    "Got cell for unknown column %s in sstable of %s.%s: ...",
    UTF8Type.instance.compose(e.columnName), metadata.ksName, metadata.cfName), e);
```

---

## How to Spot

Format strings with multiple `%s` placeholders of the same type are invisible to the compiler.  During code review, always match each placeholder position against the corresponding argument in order.  A quick sanity check is to mentally read the rendered message: "Got cell for unknown column `<ksName>` in sstable of `<cfName>.<columnName>`" immediately reveals the transposition.

---

## Test

No test added.
