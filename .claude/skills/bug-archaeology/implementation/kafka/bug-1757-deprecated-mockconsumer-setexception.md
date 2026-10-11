**Commit**: 6139840e98 · **Ticket**: KAFKA-18348
**Subsystem**: clients/src
**Category**: Race condition / missing synchronization

**Root cause**
Reviewers: TengYao Chi <kitingiao@gmail.com>, TaiJuWu <tjwu1217@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    /**
     * @deprecated Use {@link #setPollException(KafkaException)} instead
     */
    @Deprecated
    public synchronized void setException(KafkaException exception) {
        setPollException(exception);
    }


// After
                        <li>
                            The <code>setException(KafkaException)</code> method was removed from the <code>org.apache.kafka.clients.consumer.MockConsumer</code>.
                            Please use <code>setPollException(KafkaException)</code> instead.
                        </li>
```

**How to spot**
When reviewing clients/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18348 remove the deprecated mockconsumer#setexception (#18317).

**Test**
No test added.
