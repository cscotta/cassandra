**Commit**: 8c0d7412f4 · **Ticket**: KAFKA-18462
**Subsystem**: LICENSE-binary
**Category**: Logic error in condition

**Root cause**
Upgraded RocksDB from 9.7.3 to 10.1.3, deprecate two configuration in
`RocksDBGenericOptionsToDbOptionsColumnFamilyOptionsAdapter.java`
- random_access_max_buffer_size (removed since v9.11.1

https://github.com/facebook/rocksdb/commit/541761eaaac81c09f2de1325142b1d130fdd689c)
- rate_limiter (deprecated since v7.6.0

https://github.com/facebook/rocksdb/commit/25cc564ff7c396e4a9728a6dfa5fd053a5181545)

Add one configuration:
- daily_offpeak_time_utc (introduced since v9.11.1

https://github.com/facebook/rocksdb/commit/9b1d0c02e9bcf4377b0dc638441c11c4aa10e8b7)

Reviewers: Bruno Cadonna <cadonna@apache.org>

**Before/After**
```java
// Before
- rocksdbjni-9.7.3
  rocksDB: "9.7.3",
    @Override
    public Options setRandomAccessMaxBufferSize(final long randomAccessMaxBufferSize) {
        dbOptions.setRandomAccessMaxBufferSize(randomAccessMaxBufferSize);
    @Override
        return dbOptions.randomAccessMaxBufferSize();

// After
- rocksdbjni-10.1.3
  rocksDB: "10.1.3",
    @Deprecated(since = "4.2", forRemoval = true)
    public Options setRandomAccessMaxBufferSize(final long ignored) {
        log.warn("random_access_max_buffer_size has been removed in RocksDB v9.11.1." +
                " See https://github.com/facebook/rocksdb/pull/13288");
    @Deprecated(since = "4.2", forRemoval = true)
        log.warn("random_access_max_buffer_size has been removed in RocksDB v9.11.1." +
                " See https://github.com/facebook/rocksdb/pull/13288");
        return 0;
    @Deprecated(since = "4.2", forRemoval = true)
        log.warn("rate_limiter has been deprecated in RocksDB v7.6.0." +
```

**How to spot**
When reviewing LICENSE-binary code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18462: upgrade rocksdb dependency from 9.7.3 to 10.1.3 (#19880).

**Test**
No test added.
