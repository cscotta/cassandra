# Bug 1923: Missing size accumulation for result metadata in prepared statement response for protocol v2

**Commit**: 36f2178196 · **Ticket**: CASSANDRA-6035
**Subsystem**: src/java/org/apache/cassandra/transport/messages/ResultMessage.java
**Category**: Logic error in condition

---

## Root Cause

In the `encodedSize` method for a `Prepared` result message, the code correctly called `ResultSet.Metadata.codec.encodedSize(prepared.resultMetadata, version)` for protocol version 2 and above, but omitted the `size +=` assignment. The computed size of the result metadata was discarded instead of being added to the running total, causing the serialized buffer to be under-allocated and producing a corrupt or truncated message when `resultMetadata` was non-trivial.

### Before / After

```java
// BEFORE
size += CBUtil.sizeOfBytes(prepared.statementId.bytes);
size += ResultSet.Metadata.codec.encodedSize(prepared.metadata, version);
if (version > 1)
    ResultSet.Metadata.codec.encodedSize(prepared.resultMetadata, version); // result discarded
return size;
```

```java
// AFTER
size += CBUtil.sizeOfBytes(prepared.statementId.bytes);
size += ResultSet.Metadata.codec.encodedSize(prepared.metadata, version);
if (version > 1)
    size += ResultSet.Metadata.codec.encodedSize(prepared.resultMetadata, version);
return size;
```

---

## How to Spot

When reviewing size-calculation methods that accumulate into a local variable, check every branch and conditional block to ensure all computed sub-sizes are actually added with `+=`. A bare method call that returns a value (with no assignment) inside an accumulation loop is almost always a bug.

---

## Test

No test added.
