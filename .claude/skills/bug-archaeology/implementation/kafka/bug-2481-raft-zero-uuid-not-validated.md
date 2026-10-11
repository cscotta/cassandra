**Commit**: 25819cecdb · **Ticket**: KAFKA-17426
**Subsystem**: core/KafkaRaftClient
**Category**: Missing null/bounds check

**Root cause**
`KafkaRaftClient` accepted a `nodeDirectoryId` parameter in its constructor but did not validate that it was not `Uuid.ZERO_UUID`. Passing the zero UUID would produce a functionally invalid node identity that could cause subtle metadata corruption or mismatch errors at runtime without a clear diagnostic. The fix adds an explicit `IllegalArgumentException` check at construction time so the misconfiguration is caught immediately with a descriptive message.

**Before/After**
```java
// Before: no validation of nodeDirectoryId
public KafkaRaftClient(..., Uuid nodeDirectoryId, ...) {
    this.nodeDirectoryId = nodeDirectoryId;
    // ZERO_UUID silently accepted
}

// After
if (nodeDirectoryId.equals(Uuid.ZERO_UUID)) {
    throw new IllegalArgumentException(
        "nodeDirectoryId must not be ZERO_UUID");
}
this.nodeDirectoryId = nodeDirectoryId;
```

**How to spot**
Sentinel or zero values of UUID/long identifiers are often used as "unset" markers in serialization but are invalid as runtime node identities. Constructors that accept identity parameters should validate them eagerly with a clear exception rather than propagating an invalid value that will cause obscure failures later.

**Test**
Unit test added to `KafkaRaftClientTest` asserting that construction with `Uuid.ZERO_UUID` throws `IllegalArgumentException`.
