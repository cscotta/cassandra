**Commit**: ee3cea05aa · **Ticket**: KAFKA-16816
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Lianet Magrans <lmagrans@confluent.io>

**Before/After**
```java
// Before
     * @throws org.apache.kafka.common.errors.FencedInstanceIdException if this consumer instance gets fenced by broker.
     * @throws org.apache.kafka.common.errors.FencedInstanceIdException if this consumer instance gets fenced by broker.
     * @throws org.apache.kafka.common.errors.FencedInstanceIdException if this consumer instance gets fenced by broker.
     * @throws org.apache.kafka.common.errors.FencedInstanceIdException if this consumer instance gets fenced by broker.
     * @throws org.apache.kafka.common.errors.FencedInstanceIdException if this consumer instance gets fenced by broker.
     * @throws org.apache.kafka.common.errors.FencedInstanceIdException if this consumer instance gets fenced by broker.
     * @throws org.apache.kafka.common.errors.FencedInstanceIdException if this consumer instance gets fenced by broker.

// After
     * @throws org.apache.kafka.common.errors.FencedInstanceIdException if this consumer is using the classic group protocol
     *            and this instance gets fenced by broker.
     * @throws org.apache.kafka.common.errors.FencedInstanceIdException if this consumer is using the classic group protocol
     *            and this instance gets fenced by broker.
     * @throws org.apache.kafka.common.errors.FencedInstanceIdException if this consumer is using the classic group protocol
     *            and this instance gets fenced by broker.
     * @throws org.apache.kafka.common.errors.FencedInstanceIdException if this consumer is using the classic group protocol
     *            and this instance gets fenced by broker.
     * @throws org.apache.kafka.common.errors.FencedInstanceIdException if this consumer is using the classic group protocol
     *            and this instance gets fenced by broker.
     * @throws org.apache.kafka.common.errors.FencedInstanceIdException if this consumer is using the classic group protocol
     *             and this instance gets fenced by broker.
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16816: remove unneeded fencedinstanceid support on commit path for new consumer (#17559).

**Test**
Test modified in AsyncKafkaConsumerTest.java covering the corrected behaviour.
