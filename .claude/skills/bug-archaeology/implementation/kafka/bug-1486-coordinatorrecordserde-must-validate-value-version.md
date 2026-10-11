**Commit**: bf05d2c914 · **Ticket**: KAFKA-18672
**Subsystem**: coordinator-common/src
**Category**: Logic error in condition

**Root cause**
CoordinatorRecordSerde does not validate the version of the value to check whether the version is supported by the current version of the software. This is problematic if a future and unsupported version of the record is read by an older version of the software because it would misinterpret the bytes. Hence CoordinatorRecordSerde must throw an error if the version is unknown. This is also consistent with the handling in the old coordinator.

**Before/After**
```java
// Before
    /**
     * UnknownRecordTypeException is thrown when the Deserializer encounters
     * an unknown record type.
     */
    class UnknownRecordTypeException extends RuntimeException {
        private final short unknownType;

        public UnknownRecordTypeException(short unknownType) {
            super(String.format("Found an unknown record type %d", unknownType));
            this.unknownType = unknownType;
        }


// After

        if (valueVersion < valueMessage.lowestSupportedVersion() || valueVersion > valueMessage.highestSupportedVersion()) {
            throw new UnknownRecordVersionException(recordType, valueVersion);
        }

    /**
     * UnknownRecordTypeException is thrown when the Deserializer encounters
     * an unknown record type.
     */
    class UnknownRecordTypeException extends RuntimeException {
        private final short unknownType;

```

**How to spot**
When reviewing coordinator-common/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18672; coordinatorrecordserde must validate value version (#18749).

**Test**
Test modified in CoordinatorLoaderImplTest.scala covering the corrected behaviour.
