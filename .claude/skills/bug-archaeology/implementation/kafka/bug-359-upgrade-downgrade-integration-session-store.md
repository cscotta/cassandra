**Commit**: cae9e80343 · **Ticket**: KAFKA-20301
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Adds an integration tests for session store migration (plain
SessionStore  to SessionStoreWithHeaders), proxy mode, and downgrade
scenarios. Updates changelog restoration for session stores with headers by  adding
a new rawValueToSessionHeadersValue() converter. Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before

                    swallow.printStackTrace();
                    System.err.println(swallow.getMessage());
                    swallow.printStackTrace();
                    System.err.println(swallow.getMessage());
                    swallow.printStackTrace();
                    System.err.println(swallow.getMessage());
                    swallow.printStackTrace();
                    System.err.println(swallow.getMessage());
                    swallow.printStackTrace();
                    System.err.

// After
import org.apache.kafka.streams.kstream.internals.SessionWindow;
import org.apache.kafka.streams.state.AggregationWithHeaders;
import org.apache.kafka.streams.state.ReadOnlySessionStore;
import org.apache.kafka.streams.state.SessionStore;
import org.apache.kafka.streams.state.SessionStoreWithHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.atomic.AtomicReference;
    private static final String SESSION_STORE_NAME = "session-store";
    private static final Logger LOG = LoggerFactory.getLogger(HeadersStoreUpgradeIntegrationTest.class);
                    LOG.error("Error while checking store result", swallow);
                    LOG.error("Error while verifying legacy value", swallow);
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20301: upgrade downgrade integration tests for session store with headers (#21748).

**Test**
Test modified in HeadersStoreUpgradeIntegrationTest.java covering the corrected behaviour.
