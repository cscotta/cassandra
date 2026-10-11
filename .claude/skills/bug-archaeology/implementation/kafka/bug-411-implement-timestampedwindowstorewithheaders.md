**Commit**: 5fc5d7591c · **Ticket**: KAFKA-20134
**Subsystem**: streams/integration-tests
**Category**: Logic error in condition

**Root cause**
This PR implements the upgrade integration tests for
`TimestampedWindowStateStoreWithHeaders` introduced in KIP-1271. The class should be reviewd:  `HeadersStoreUpgradeIntegrationTest`

This should not be merged before #21497

Reviewers: Matthias J. Sax <matthias@confluent.io>, Alieh Saeedi
<asaeedi@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After
import org.apache.kafka.streams.errors.ProcessorStateException;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.ReadOnlyWindowStore;
import org.apache.kafka.streams.state.TimestampedWindowStore;
import org.apache.kafka.streams.state.TimestampedWindowStoreWithHeaders;
import java.util.LinkedList;
import java.util.List;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
    private static final String WINDOW_STORE_NAME = "window-store";
```

**How to spot**
When reviewing streams/integration-tests code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20134: implement timestampedwindowstorewithheaders (n/n) (#21581).

**Test**
Test modified in HeadersStoreUpgradeIntegrationTest.java covering the corrected behaviour.
