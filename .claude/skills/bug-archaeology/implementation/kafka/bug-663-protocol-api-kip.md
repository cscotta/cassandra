**Commit**: 05c9322ba9 · **Ticket**: KAFKA-19814
**Subsystem**: clients/src
**Category**: Wrong serialization / deserialization

**Root cause**
Defines the updates protocol RPC schemas for KIPs 1206 and 1222 ready
for Apache Kafka 4.2. Defines `AcknowledgeType.RENEW` and the new
`ShareConsumer.acquisitionLockTimeoutMs()` method. Reviewers: Apoorv Mittal <apoorvmittal10@gmail.com>, Sushant Mahajan
 <smahajan@confluent.io>

**Before/After**
```java
// Before
    REJECT((byte) 3);

// After
    REJECT((byte) 3),

    /** The record is still being processed. Renew the acquisition lock so processing can continue. */
    RENEW((byte) 4);
            case 4:
                return RENEW;
    /**
     * Returns the acquisition lock timeout for the last set of records fetched from the cluster.
     *
     * @return The acquisition lock timeout in milliseconds, or {@code Optional.empty()} if the timeout is not known.
     */
    @Override
```

**How to spot**
When reviewing clients/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19814: protocol and api changes for kip-1222 (#20732).

**Test**
No test added.
