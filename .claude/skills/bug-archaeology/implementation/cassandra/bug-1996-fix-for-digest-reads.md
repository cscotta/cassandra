# Bug 1996: ReadResponseSerializer Reports Wrong Size for Digest Reads

**Commit**: 57eb87b57c · **Ticket**: CASSANDRA-5476
**Subsystem**: src/java/org/apache/cassandra/db/ReadResponse.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

`ReadResponseSerializer.serializedSize()` computed the size of a digest read response by adding the length-prefix size for the buffer (`sizeof(buffer.remaining())`) but forgot to add the actual buffer bytes (`buffer.remaining()` itself). This caused the calculated serialized size to be shorter than the actual bytes written, which could break framing in the messaging layer or cause assertion failures when the caller relied on `serializedSize()` matching the written output.

### Before / After

```java
// BEFORE
int size = typeSizes.sizeof(buffer.remaining());  // only length prefix counted
size += typeSizes.sizeof(response.isDigestQuery());
```

```java
// AFTER
int size = typeSizes.sizeof(buffer.remaining());
size += buffer.remaining();                        // actual digest bytes added
size += typeSizes.sizeof(response.isDigestQuery());
```

---

## How to Spot

In any `serializedSize()` implementation, every field written during `serialize()` must have a corresponding size contribution. Variable-length byte buffers require two terms: the encoded length of the length-prefix and the length of the data itself. Auditing `serialize()` and `serializedSize()` side-by-side line-by-line catches these omissions.

---

## Test

No test added.
