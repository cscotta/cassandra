# Bug 1981: Multiple native protocol v2 correctness bugs — paging state buffer not flipped, spurious HAS_MORE_PAGES on last page, and skip-metadata not applied

**Commit**: 57769f6bb5 · **Ticket**: CASSANDRA-5770
**Subsystem**: cql3/ResultSet.java, service/pager/PagingState.java, transport/messages/ExecuteMessage.java, transport/messages/QueryMessage.java, cql3/statements/SelectStatement.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

Three distinct bugs existed in the native protocol v2 implementation. First, `PagingState.serialize()` wrote into a `ByteBuffer` but forgot to call `result.flip()` before returning it, so the buffer's position was at the end and the serialized bytes were unreadable by the recipient. Second, `SelectStatement` called `setHasMorePages(pager.state())` unconditionally even on the last page when the pager was exhausted, causing clients to issue a spurious extra round-trip. Third, the `skipMetadata` flag from QUERY and EXECUTE messages was parsed but never applied to the response, so metadata was always included even when the client explicitly requested it be omitted.

### Before / After

```java
// BEFORE: buffer not flipped before return
ByteBufferUtil.writeWithShortLength(partitionKey, out);
ByteBufferUtil.writeWithShortLength(cellName, out);
out.writeInt(remaining);
return result;  // position at end, no readable bytes
```

```java
// AFTER:
out.writeInt(remaining);
result.flip();  // position reset to 0, limit set to written bytes
return result;
```

```java
// BEFORE: HAS_MORE_PAGES set even when pager exhausted
msg.result.metadata.setHasMorePages(pager.state());
```

```java
// AFTER:
if (!pager.isExhausted())
    msg.result.metadata.setHasMorePages(pager.state());
```

---

## How to Spot

When serializing into a `ByteBuffer` via an output stream wrapper, always verify `flip()` is called before the buffer is read or returned. For paging, always gate `HAS_MORE_PAGES` on an explicit exhaustion check rather than assuming a non-null paging state implies there are more results.

---

## Test

No test added.
