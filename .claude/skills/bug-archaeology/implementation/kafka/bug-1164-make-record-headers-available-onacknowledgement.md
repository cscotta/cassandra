**Commit**: ae771d73d1 · **Ticket**: KAFKA-8830
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Two sets of tests are added:
1. KafkaProducerTest
- when send success, both record.headers() and onAcknowledgement headers
are read only
- when send failure, record.headers() is writable as before and
onAcknowledgement headers is read only
2. ProducerInterceptorsTest
- make both old and new onAcknowledgement method are called successfully

Reviewers: Lianet Magrans <lmagrans@confluent.io>, Omnia Ibrahim
<o.g.h.ibrahim@gmail.com>, Matthias J. Sax <matthias@confluent.io>,
Andrew Schofield <aschofield@confluent.io>, Chia-Ping Tsai
<chia7712@gmail.com>

**Before/After**
```java
// Before
            this.interceptors.onAcknowledgement(metadata, exception);
     *                 before partition gets assigned, then partition will be set to RecordMetadata.NO_PARTITION.
    void onAcknowledgement(RecordMetadata metadata, Exception exception);

// After
        private final Headers headers;
            if (record != null) {
                headers = record.headers();
            } else {
                headers = new RecordHeaders();
                ((RecordHeaders) headers).setReadOnly();
            }
            this.interceptors.onAcknowledgement(metadata, exception, headers);
import org.apache.kafka.common.header.Headers;
     *                 before partition gets assigned, then partition will be set to {@link RecordMetadata#UNKNOWN_PARTITION}.
    default void onAcknowledgement(RecordMetadata metadata, Exception exception) {}

```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-8830 make record headers available in onacknowledgement (#17099).

**Test**
Test modified in KafkaProducerTest.java covering the corrected behaviour.
