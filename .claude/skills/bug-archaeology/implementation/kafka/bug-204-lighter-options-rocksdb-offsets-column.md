**Commit**: 30cea6f2e8 · **Ticket**: KAFKA-20456
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
This change gives the offsets column family its own lightweight
`ColumnFamilyOptions` instead of sharing the data CF's options. The
offsets CF stores only a handful of key-value pairs (one per changelog
partition) — it doesn't need  the large write buffers, bloom filters, or
aggressive compaction configured for the data CF. Reviewers: Nick Telford <nick.telford@gmail.com>, Matthias J. Sax
 <matthias@confluent.io>

**Before/After**
```java
// Before
            new ColumnFamilyDescriptor(OFFSETS_COLUMN_FAMILY_NAME, columnFamilyOptions)
            new ColumnFamilyDescriptor(OFFSETS_COLUMN_FAMILY_NAME, columnFamilyOptions)
                new ColumnFamilyDescriptor(OFFSETS_COLUMN_FAMILY_NAME, columnFamilyOptions)
            new ColumnFamilyDescriptor(OFFSETS_COLUMN_FAMILY_NAME, columnFamilyOptions)

// After
            new ColumnFamilyDescriptor(OFFSETS_COLUMN_FAMILY_NAME, createOffsetsCFOptions())
            new ColumnFamilyDescriptor(OFFSETS_COLUMN_FAMILY_NAME, createOffsetsCFOptions())
    /**
     * Creates lightweight {@link ColumnFamilyOptions} for the offsets column family. The offsets CF
     * stores only a small number of key-value pairs (one per changelog partition), so it does not
     * need the heavyweight options used for the data CF (large write buffers, bloom filters,
     * aggressive compaction). Sharing the data CF's options causes unnecessary write amplification
     * and compaction pressure that can contribute to RocksDB write stalls under heavy restore I/O.
     */
    protected static ColumnFamilyOptions createOffsetsCFOptions() {
        final ColumnFamilyOptions offsetsCFOptions = new ColumnFamilyOptions();
        offsetsCFOptions.setCompressionType(CompressionType.NO_COMPRESSION);
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20456: use lighter options for rocksdb offsets column family (#22085).

**Test**
No test added.
