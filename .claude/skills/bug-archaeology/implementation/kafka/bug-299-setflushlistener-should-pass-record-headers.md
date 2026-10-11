**Commit**: 3fd16d0c9b · **Ticket**: KAFKA-20203
**Subsystem**: streams/src
**Category**: Resource leak

**Root cause**
Make `setFlushListener` pass `record.headers()` instead of empty
headers. Already done correctly for kv- and session-store case. This PR
closes the gap  for window-store case. Reviewers: Matthias J.

**Before/After**
```java
// Before
                            record.value().newValue != null ? serdes.valueFrom(record.value().newValue, new RecordHeaders()) : null,
                            record.value().oldValue != null ? serdes.valueFrom(record.value().oldValue, new RecordHeaders()) : null,

// After
                            record.value().newValue != null ? serdes.valueFrom(record.value().newValue, record.headers()) : null,
                            record.value().oldValue != null ? serdes.valueFrom(record.value().oldValue, record.headers()) : null,
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.utils.LogContext;
import org.apache.kafka.common.utils.Time;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.processor.internals.ProcessorRecordContext;
import org.apache.kafka.test.InternalMockProcessorContext;
import org.apache.kafka.test.MockRecordCollector;
import org.apache.kafka.test.StreamsTestUtils;
import org.apache.kafka.test.TestUtils;
```

**How to spot**
When reviewing streams/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20203: setflushlistener should pass record.headers() instead of empty headers (#21802).

**Test**
Test modified in MeteredKeyValueStoreTest.java covering the corrected behaviour.
