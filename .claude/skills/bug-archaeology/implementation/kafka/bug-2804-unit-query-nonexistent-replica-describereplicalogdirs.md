**Commit**: 8a109d87d1 · **Ticket**: KAFKA-17009
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        // The current log directory of the replica of this partition on the given broker.
        // Null if no replica is not found for this partition on the given broker.
        // Defined as max(HW of partition - LEO of the replica, 0).
        // The future log directory of the replica of this partition on the given broker.
        // Null if the replica of this partition is not being moved to another log directory on the given broker.
        // The LEO of the replica - LEO of the future log of this replica in the destination log directory.
        // -1 if either there is not replica for this partition or the replica of this partition is not being moved to another log directory on the given broker.

// After

        /**
         * The current log directory of the replica of this partition on the given broker.
         * Null if no replica is not found for this partition on the given broker.
         */
        /**
         * Defined as max(HW of partition - LEO of the replica, 0).
         */
        /**
         * The future log directory of the replica of this partition on the given broker.
         * Null if the replica of this partition is not being moved to another log directory on the given broker.
         */
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17009 add unit test to query nonexistent replica by describereplicalogdirs (#16423).

**Test**
Test modified in KafkaAdminClientTest.java covering the corrected behaviour.
