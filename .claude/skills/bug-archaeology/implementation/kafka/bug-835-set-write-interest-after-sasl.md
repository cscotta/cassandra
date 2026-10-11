**Commit**: f73e3bcd6a · **Ticket**: KAFKA-19561
**Subsystem**: clients/src
**Category**: Race condition / missing synchronization

**Root cause**
https://issues.apache.org/jira/browse/KAFKA-19561

Addresses a race condition during SASL reauthentication where the
server-side `KafkaChannel.send()` queues a response, but OP_WRITE is
removed before the channel becomes writable — resulting in stuck
responses and client  timeouts. Reviewers: Rajini Sivaram <rajinisivaram@googlemail.com>

**Before/After**
```java
// Before
// (no removals)

// After


    /**
     * Maybe add write interest after re-authentication. This is to ensure that any pending write operation
     * is resumed.
     */
    public void maybeAddWriteInterestAfterReauth() {
        if (send != null)
            this.transportLayer.addInterestOps(SelectionKey.OP_WRITE);
    }
                            channel.maybeAddWriteInterestAfterReauth();
import org.apache.kafka.common.network.NetworkReceive;
```

**How to spot**
When reviewing clients/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19561: set op_write interest after sasl reauthentication to resume pending writes (#20258).

**Test**
Test modified in SaslAuthenticatorTest.java covering the corrected behaviour.
