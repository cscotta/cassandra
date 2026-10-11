# Bug 2530: ReadResponse serializedSize double-counts the digest bytes

**Commit**: c4fabbbb30 · **Ticket**: CASSANDRA-3373

## Detection Rule
When implementing `serializedSize()` to pre-compute message sizes, trace each conditional branch in the corresponding `serialize()` method and ensure each field is counted exactly once; a field listed both before and inside a conditional branch is counted twice.

## Code Smell
`size += (response.isDigestQuery() ? response.digest() : ...).remaining(); size += DBConstants.boolSize; if (response.isDigestQuery()) size += response.digest().remaining();` — the digest length is added unconditionally (via the ternary) and then added again inside the `if (isDigestQuery)` branch, causing the declared size to be larger than the actual serialized bytes.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: double-counted-size-field
- severity: crash
- subsystem: network-messaging

## Root Cause
`ReadResponseSerializer.serializedSize()` computed the size as: `intSize` + `digest-or-empty-buffer length` + `boolSize` + (if digest query: `digest length` again). This made the declared size 4 bytes larger than the actual serialization for digest responses. Callers that allocate a buffer based on `serializedSize()` would allocate too much, causing framing errors or assertion failures when the size check on the read side found fewer bytes than expected.

## Example
```java
// BEFORE — digest counted twice
int size = DBConstants.intSize;
size += (response.isDigestQuery()
    ? response.digest()
    : ByteBufferUtil.EMPTY_BYTE_BUFFER).remaining(); // counted here
size += DBConstants.boolSize;
if (response.isDigestQuery())
    size += response.digest().remaining();            // and again here
```
```java
// AFTER — digest counted once
int size = DBConstants.intSize;
size += DBConstants.boolSize;
if (response.isDigestQuery())
    size += response.digest().remaining();
```
