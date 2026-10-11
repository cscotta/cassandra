**Commit**: 1407b12e2f · **Ticket**: KAFKA-19313
**Subsystem**: storage/src
**Category**: Logic error in condition

**Root cause**
Replaces the UNIFIED_LOG_UNKNOWN_OFFSET constant in LogOffsetMetadata
with UnifiedLog.UNKNOWN_OFFSET. Reviewers: PoAn Yang <payang@apache.org>, Ken Huang
<s7133700@gmail.com>, YuChia Ma <minecraftmiku831@gmail.com>, Chia-Ping
Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    //TODO KAFKA-14484 remove once UnifiedLog has been moved to the storage module
    private static final long UNIFIED_LOG_UNKNOWN_OFFSET = -1L;
    public static final LogOffsetMetadata UNKNOWN_OFFSET_METADATA = new LogOffsetMetadata(-1L, UNIFIED_LOG_UNKNOWN_OFFSET, UNKNOWN_FILE_POSITION);
        this(messageOffset, UNIFIED_LOG_UNKNOWN_OFFSET, UNKNOWN_FILE_POSITION);
        return segmentBaseOffset == UNIFIED_LOG_UNKNOWN_OFFSET && relativePositionInSegment == UNKNOWN_FILE_POSITION;

// After
    public static final LogOffsetMetadata UNKNOWN_OFFSET_METADATA = new LogOffsetMetadata(-1L, UnifiedLog.UNKNOWN_OFFSET, UNKNOWN_FILE_POSITION);
        this(messageOffset, UnifiedLog.UNKNOWN_OFFSET, UNKNOWN_FILE_POSITION);
        return segmentBaseOffset == UnifiedLog.UNKNOWN_OFFSET && relativePositionInSegment == UNKNOWN_FILE_POSITION;
```

**How to spot**
When reviewing storage/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19313 replace logoffsetmetadata#unified_log_unknown_offset by unifiedlog.unknown_offset (#19767).

**Test**
No test added.
