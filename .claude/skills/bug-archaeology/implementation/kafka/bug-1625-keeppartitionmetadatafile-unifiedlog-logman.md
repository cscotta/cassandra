**Commit**: 92fd99bda1 · **Ticket**: KAFKA-18479
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Jun Rao <junrao@gmail.com>

**Before/After**
```java
// Before
    private boolean keepPartitionMetadataFile = true;
    public LogManagerBuilder setKeepPartitionMetadataFile(boolean keepPartitionMetadataFile) {
        this.keepPartitionMetadataFile = keepPartitionMetadataFile;
        return this;
    }

                              keepPartitionMetadataFile,
                 val keepPartitionMetadataFile: Boolean,
      keepPartitionMetadataFile = keepPartitionMetadataFile,
          keepPartitionMetadataFile = keepPartitionMetadataFile,
            logDirFailureChannel: LogDirFailureChannel,
            keepPartitionMetadataFile: Boolean): LogManager = {

// After
            logDirFailureChannel: LogDirFailureChannel): LogManager = {
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18479: remove keeppartitionmetadatafile in unifiedlog and logman… (#18491).

**Test**
Test modified in PartitionLockTest.scala covering the corrected behaviour.
