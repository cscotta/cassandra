**Commit**: 786d2c9975 · **Ticket**: KAFKA-17276
**Subsystem**: clients/src
**Category**: Wrong serialization / deserialization

**Root cause**
The replicaDirectoryId field for FetchRequest and FetchSnapshotRequest should be ignorable. This allows data objects with the directory id to be serialized to any version of the requests. Reviewers: José Armando García Sancio <jsancio@apache.org>, Chia-Ping Tsai <chia7712@apache.org>

**Before/After**
```java
// Before
        { "name": "ReplicaDirectoryId", "type": "uuid", "versions": "17+", "taggedVersions": "17+", "tag": 0,
            { "name": "ReplicaDirectoryId", "type": "uuid", "versions": "1+", "taggedVersions": "1+", "tag": 0,

// After
        { "name": "ReplicaDirectoryId", "type": "uuid", "versions": "17+", "taggedVersions": "17+", "tag": 0, "ignorable": true,
            { "name": "ReplicaDirectoryId", "type": "uuid", "versions": "1+", "taggedVersions": "1+", "tag": 0, "ignorable": true,
```

**How to spot**
When reviewing clients/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17276; replicadirectoryid for fetch and fetchsnapshot should be ignorable (#16819).

**Test**
No test added.
