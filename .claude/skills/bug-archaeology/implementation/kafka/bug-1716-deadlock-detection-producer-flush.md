**Commit**: abeed20168 · **Ticket**: KAFKA-10790
**Subsystem**: clients/src
**Category**: Deadlock

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>, Andrew Schofield <aschofield@confluent.io>, TaiJuWu <tjwu1217@gmail.com>

**Before/After**
```java
// Before
     * <b>Important:</b> This method should not be used within the callback provided to
     * {@link #send(ProducerRecord, Callback)}. Invoking <code>flush()</code> in this context will cause a deadlock.
            log.error("KafkaProducer.flush() invocation inside a callback will cause a deadlock.");

// After
     * <b>Important:</b> This method must not be called from within the callback provided to
     * {@link #send(ProducerRecord, Callback)}. Invoking <code>flush()</code> in this context will result in a
     * {@link KafkaException} being thrown, as it will cause a deadlock.
     * @throws KafkaException If the method is invoked inside a {@link #send(ProducerRecord, Callback)} callback
            log.error("KafkaProducer.flush() invocation inside a callback is not permitted because it may lead to deadlock.");
            throw new KafkaException("KafkaProducer.flush() invocation inside a callback is not permitted because it may lead to deadlock.");
    @Test
    public void shouldNotInvokeFlushInCallback() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9000");
        // only test in idempotence disabled producer for simplicity
        configs.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, false);
```

**How to spot**
When reviewing clients/src code, look for deadlock patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-10790: add deadlock detection to producer#flush (#17946).

**Test**
Test modified in KafkaProducerTest.java covering the corrected behaviour.
