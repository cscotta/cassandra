**Commit**: df73133f3b · **Ticket**: KAFKA-19080
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
See Discussion:
https://github.com/apache/kafka/pull/19371#discussion_r2109549343

Do the following changes:
- Update the internal config name with metadata prefix
- add the warning message for setting
`INTERNAL_METADATA_LOG_SEGMENT_BYTES_CONFIG`

Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
      MetadataLogConfig.INTERNAL_MAX_BATCH_SIZE_IN_BYTES_CONFIG, internalMaxBatchSizeInBytes,
      MetadataLogConfig.INTERNAL_MAX_FETCH_SIZE_IN_BYTES_CONFIG, internalMaxFetchSizeInBytes,
      MetadataLogConfig.INTERNAL_DELETE_DELAY_MILLIS_CONFIG, internalDeleteDelayMillis,
        case MetadataLogConfig.INTERNAL_MAX_BATCH_SIZE_IN_BYTES_CONFIG => // no op
        case MetadataLogConfig.INTERNAL_MAX_FETCH_SIZE_IN_BYTES_CONFIG => // no op
        case MetadataLogConfig.INTERNAL_DELETE_DELAY_MILLIS_CONFIG => // no op
    public static final String INTERNAL_MAX_BATCH_SIZE_IN_BYTES_CONFIG = "internal.max.batch.size.in.bytes";
    public static final String INTERNAL_MAX_BATCH_SIZE_IN_BYTES_DOC = "The largest record batch size allowed in the metadata log, only for testing.";
    public static final String INTERNAL_MAX_FETCH_SIZE_IN_BYTES_CONFIG = "internal.max.fetch.size.in.bytes";
    public static final String INTERNAL_MAX_FETCH_SIZE_IN_BYTES_DOC = "The maximum number of bytes to read when fetching from the metadata log, only for testing.";
    public static final String INTERNAL_DELETE_DEL

// After
    if (defaultLogConfig.segmentSize() < config.logSegmentBytes()) {
      metadataLog.error(s"Overriding ${MetadataLogConfig.INTERNAL_METADATA_LOG_SEGMENT_BYTES_CONFIG} is only supported for testing. Setting " +
        s"this value too low may lead to an inability to write batches of metadata records.")
    }

      MetadataLogConfig.INTERNAL_METADATA_MAX_BATCH_SIZE_IN_BYTES_CONFIG, internalMaxBatchSizeInBytes,
      MetadataLogConfig.INTERNAL_METADATA_MAX_FETCH_SIZE_IN_BYTES_CONFIG, internalMaxFetchSizeInBytes,
      MetadataLogConfig.INTERNAL_METADATA_DELETE_DELAY_MILLIS_CONFIG, internalDeleteDelayMillis,
        case MetadataLogConfig.INTERNAL_METADATA_MAX_BATCH_SIZE_IN_BYTES_CONFIG => // no op
        case MetadataLogConfig.INTERNAL_METADATA_MAX_FETCH_SIZE_IN_BYTES_CONFIG => // no op
        case MetadataLogConfig.INTERNAL_METADATA_DELETE_DELAY_MILLIS_CONFIG => // no op
    public static final String INTERNAL_METADATA_MAX_BATCH_SIZE_IN_BYTES_CONFIG = "internal.metadata.max.batch.size.in.bytes";
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: minor: follow up kafka-19080 metadatalogconfig (#19842).

**Test**
Test modified in KafkaMetadataLogTest.scala covering the corrected behaviour.
