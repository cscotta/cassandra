**Commit**: 3c965e4e0b · **Ticket**: KAFKA-20096
**Subsystem**: docs/streams
**Category**: Wrong serialization / deserialization

**Root cause**
Document the limitation we have in RocksDB upgrade due to file format
version change. Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
Downgrading from 3.0.x or newer version to 2.8.x or older version needs special attention: Since 3.0.0 release, Kafka Streams uses a newer RocksDB version whose on-disk format changed. This means that old versioned RocksDB would not be able to recognize the bytes written by that newer versioned RocksDB, and hence it is harder to downgrade Kafka Streams with version 3.0.0 or newer to older versions in-flight. Users need to wipe out the local RocksDB state stores written by the new versioned Kafka Streams before swapping in the older versioned Kafka Streams bytecode, which would then restore the state stores with the old on-disk format from the changelogs.

// After
Downgrading from 3.0.x or newer version to 2.8.x or older version needs special attention: Since 3.0.0 release, Kafka Streams uses a newer RocksDB version whose on-disk format changed. This means that old versioned RocksDB would not be able to recognize the bytes written by that newer versioned RocksDB, and hence it is harder to downgrade Kafka Streams with version 3.0.0 or newer to older versions in-flight. Users need to first delete the local RocksDB state stores written by the new versioned Kafka Streams before swapping in the older versioned Kafka Streams bytecode, which would then restore the state stores with the old on-disk format from the changelogs.

Downgrading from 4.0.x or newer version to older than 4.0.0 needs special attention: Since 4.0.0 release, Kafka Streams upgraded RocksDB from version 7.9.2 to 9.7.3. This upgrade introduces a RocksDB file format version change from version 5 to version 6 (introduced in RocksDB 8.6). While the newer RocksDB version (9.7.3) can read state stores written in the old format (version 5), the older RocksDB versions cannot read state stores written in the new format (version 6). This means that downgrading from Kafka Streams 4.0.x or newer to versions older than 4.0.0 in-flight is not possible out-of-the-box. Users must first delete the local RocksDB state stores written by Kafka Streams 4.0.x or newer before downgrading to versions older than 4.0.0, which will then restore the state stores from the changelogs using the old file format.
```

**How to spot**
When reviewing docs/streams code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20096: add documentation about the incompatibility of rocksdb file format (#21397).

**Test**
No test added.
