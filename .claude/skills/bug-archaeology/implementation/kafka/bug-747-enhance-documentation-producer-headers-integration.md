**Commit**: 57e9f98e15 · **Ticket**: KAFKA-19644
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
- Improve the docs for Record Headers. - Add integration tests to verify that the order of headers in a record
is preserved when producing and consuming. - Add unit tests for RecordHeaders.java. Reviewers: Ken Huang <s7133700@gmail.com>, Hong-Yi Chen
 <apalan60@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
            for (ConsumerRecord<byte[], byte[]> consumerRecord : records) {
                Header header = consumerRecord.headers().lastHeader("headerKey");
                if (header != null)
                    assertEquals("headerValue", new String(header.value()));
            }



// After
            record.headers().add("headerKey2", "headerValue2".getBytes());
            record.headers().add("headerKey3", "headerValue3".getBytes());
            producer.flush();


            // Test the order of headers in a record is preserved when producing and consuming
            Header[] headers = records.get(0).headers().toArray();
            assertEquals("headerKey", headers[0].key());
            assertEquals("headerKey2", headers[1].key());
            assertEquals("headerKey3", headers[2].key());
            record.headers().add("headerKey2", "headerValue2".getBytes());
            record.headers().add("headerKey3", "headerValue3".getBytes());
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19644 enhance the documentation for producer headers and integration tests (#20524).

**Test**
Test modified in PlaintextConsumerTest.java covering the corrected behaviour.
