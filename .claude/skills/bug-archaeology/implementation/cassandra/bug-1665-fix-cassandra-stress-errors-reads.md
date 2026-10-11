# Bug 1665: Fix Cassandra Stress Errors on Reads with Native Protocol

**Commit**: a16adba9b8 · **Ticket**: CASSANDRA-7033
**Subsystem**: tools/stress/src/org/apache/cassandra/stress/operations/
**Category**: Wrong serialization / deserialization

---

## Root Cause

The cassandra-stress CQL reader incorrectly handled result rows when using the native (CQL3) protocol. The code assumed the first column in every result row was the partition key and should be stripped before comparison, so it started iteration at index `j=1` and allocated `size - 1` buffers. With CQL3, the key is not automatically prepended to result rows, so this off-by-one stripping produced incorrect (truncated) result arrays and false validation failures.

### Before / After

```java
// BEFORE – skips column 0, assumes it is the key
r[i] = new ByteBuffer[row.getColumnDefinitions().size() - 1];
for (int j = 1 ; j < row.getColumnDefinitions().size() ; j++)
    r[i][j - 1] = row.getBytes(j);
```

```java
// AFTER – reads all columns as-is
r[i] = new ByteBuffer[row.getColumnDefinitions().size()];
for (int j = 0 ; j < row.getColumnDefinitions().size() ; j++)
    r[i][j] = row.getBytes(j);
```

---

## How to Spot

Any code that assumes "column 0 is always the key" for CQL3 result sets is likely wrong: CQL3 native protocol result sets contain only the columns listed in the SELECT clause. Protocol-specific layout assumptions left over from Thrift/CQL2 are a common source of this class of error when adding CQL3 support.

---

## Test

No test added.
