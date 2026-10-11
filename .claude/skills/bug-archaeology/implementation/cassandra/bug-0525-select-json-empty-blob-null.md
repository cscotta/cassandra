# Bug 0525: SELECT JSON Outputs null for Empty Blobs Instead of "0x"

**Commit**: c3dc6eb57e · **Ticket**: CASSANDRA-15435
**Subsystem**: src/java/org/apache/cassandra/cql3/selection/Selection.java
**Category**: Logic error in condition

---

## Root Cause

In `Selection.resultSetBuilder`, the JSON serialization branch used `buffer == null || !buffer.hasRemaining()` to decide whether to emit `null`. The condition correctly handles a truly absent (null) column value but incorrectly treats an empty `ByteBuffer` (one where `hasRemaining()` returns false) the same way. An empty blob (`0x`) is a legitimate, non-null value in Cassandra; serializing it as `null` in JSON output corrupts the data representation and makes it impossible for clients to distinguish a null blob from an explicitly inserted empty blob.

The fix changes the condition to `buffer == null` only, allowing empty `ByteBuffer` values to fall through to the type's `toJSONString()` method, which correctly renders them as `"0x"`.

### Before / After

```java
// BEFORE — treats empty ByteBuffer as null
if (buffer == null || !buffer.hasRemaining())
    sb.append("null");
else
    sb.append(spec.type.toJSONString(buffer, protocolVersion));
```

```java
// AFTER — only truly absent values become null
if (buffer == null)
    sb.append("null");
else
    sb.append(spec.type.toJSONString(buffer, protocolVersion));
```

---

## How to Spot

When serializing nullable binary columns to a text format, `null` and empty are semantically distinct. Any guard that short-circuits to a `null` literal on both a null reference and an empty buffer conflates these two states. Review JSON/CQL serialization paths for any `== null || !hasRemaining()` pattern applied to `ByteBuffer` to ensure empty values are passed to the type serializer rather than emitted as `null`.

---

## Test

`JsonTest.testInsertAndSelectJsonSyntaxWithEmptyAndNullValues` inserts rows with empty strings, empty blobs, and null values via both JSON and CQL syntax, then asserts that `SELECT JSON` renders empty blobs as `"0x"` and null columns as `null`.
