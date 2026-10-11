**Commit**: caeca090b8 · **Ticket**: KAFKA-8862
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Clarify timeout errors received on send if the case is topic not in
metadata vs partition not in metadata. Add integration tests showcases
the difference  Follow-up from 4.1 fix for misleading timeout error
message (https://issues.apache.org/jira/browse/KAFKA-8862)

Reviewers: TengYao Chi <frankvicky@apache.org>, Kuan-Po Tseng
 <brandboat@gmail.com>

**Before/After**
```java
// Before
     * @param record The record to send
     *        indicates no callback)
     *
     * @throws IllegalStateException if a transactional.id has been configured and no transaction has been started, or
     *                               when send is invoked after producer has been closed.
     * @throws InterruptException If the thread is interrupted while blocked
     * @throws KafkaException If a Kafka related error occurs that does not belong to the public API exceptions.
     * @throws AuthorizationException if not authorized to the specified topic. See the exception for more details
     * @throws InterruptException if the thread is interrupted while blocked
     * @throws TimeoutException if metadata could not be refreshed within {@code max.block.ms}
     * @throws KafkaException for all Kafka-related exceptions, including the case where this method is called after producer close
    val overridingProps = new Properties()

// After
     * @param record   The record to send
     *                 indicates no callback)
     * @throws IllegalStateException  if a transactional.id has been configured and no transaction has been started, or
     *                                when send is invoked after producer has been closed.
     * @throws TimeoutException       if the topic or the partition specified in the record cannot be found in metadata within {@code max.block.ms}
     * @throws InterruptException     If the thread is interrupted while blocked
     * @throws KafkaException         If a Kafka related error occurs that does not belong to the public API exceptions.
     * <p/>
     * This will attempt to refresh metadata until it finds the topic in it, or the configured {@link ProducerConfig#MAX_BLOCK_MS_CONFIG} expires.
     *
     * @throws AuthorizationException  if not authorized to the specified topic. See the exception for more details
     * @throws InterruptException      if the thread is interrupted while blocked
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: minor: improve producer docs and add tests around timeout behaviour on missing topic/partition (#20533).

**Test**
Test modified in BaseProducerSendTest.scala covering the corrected behaviour.
