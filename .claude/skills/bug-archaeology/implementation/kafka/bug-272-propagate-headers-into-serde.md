**Commit**: 2713477dba · **Ticket**: KAFKA-20173
**Subsystem**: streams/src
**Category**: Wrong serialization / deserialization

**Root cause**
This PR ensures headers are propagated in key related methods from
`WindowKeySchema` and  `PrefixedWindowKeySchemas`  Related tests are
updated

Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
// TODO: replace with new method in follow-up PR of KIP-1271
@SuppressWarnings("deprecation")
            final byte[] serializedKey = serdes.rawKey(timeKey.key());
            final byte[] serializedKey = serdes.rawKey(key);
            final byte[] serializedKey = serdes.rawKey(key);
            final byte[] serializedKey = serdes.rawKey(timeKey.key());
// TODO: replace with new method in follow-up PR of KIP-1271
@SuppressWarnings("deprecation")
        final byte[] serializedKey = serdes.rawKey(key);
        final byte[] serializedKey = serdes.rawKey(timeKey.key());

// After
                                                 final Headers headers,
            final byte[] serializedKey = serdes.rawKey(timeKey.key(), headers);
                                                 final Headers headers,
            final byte[] serializedKey = serdes.rawKey(key, headers);
                                                 final Headers headers,
            final byte[] serializedKey = serdes.rawKey(key, headers);
                                                 final Headers headers,
            final byte[] serializedKey = serdes.rawKey(timeKey.key(), headers);
                                             final Headers headers,
        final byte[] serializedKey = serdes.rawKey(key, headers);
                                             final Headers headers,
        final byte[] serializedKey = serdes.rawKey(timeKey.key(), headers);
```

**How to spot**
When reviewing streams/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20173: propagate headers into serde 6/n (#21850).

**Test**
Test modified in AbstractDualSchemaRocksDBSegmentedBytesStoreTest.java covering the corrected behaviour.
