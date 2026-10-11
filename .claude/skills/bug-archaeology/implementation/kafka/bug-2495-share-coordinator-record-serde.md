**Commit**: 1621f88f06 · **Ticket**: KAFKA-17367
**Subsystem**: coordinator-common / share-coordinator
**Category**: Wrong serialization/deserialization

**Root cause**
`GroupCoordinatorRecordSerde` duplicated the binary format logic (version-prefixed key and value serialization/deserialization) that will also be needed by the new `ShareCoordinatorRecordSerde`. Without a shared base class, any future fix to the serialization format would need to be applied independently to both classes. The fix extracts the common framing logic into `CoordinatorRecordSerde` in the `coordinator-common` module and has `GroupCoordinatorRecordSerde` extend it, leaving only the `apiMessageKeyFor` / `apiMessageValueFor` dispatch as abstract methods for each coordinator to implement.

**Before/After**
```java
// Before: GroupCoordinatorRecordSerde implemented Serializer and Deserializer directly
// with all framing logic inline

// After: abstract base in coordinator-common
public abstract class CoordinatorRecordSerde
        implements Serializer<CoordinatorRecord>, Deserializer<CoordinatorRecord> {
    // shared framing logic
    protected abstract ApiMessage apiMessageKeyFor(short recordVersion);
    protected abstract ApiMessage apiMessageValueFor(short recordVersion);
}
```

**How to spot**
When introducing a second coordinator that uses the same record wire format as an existing one, extract the framing into an abstract base class before copying the code. Duplicated serialization logic is particularly dangerous because divergence causes data that one coordinator can write but the other cannot read.

**Test**
`GroupCoordinatorRecordSerdeTest` updated; new `ShareCoordinatorRecordSerdeTest` validates the shared base.
