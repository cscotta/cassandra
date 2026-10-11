**Commit**: 8bda306139 · **Ticket**: KAFKA-20277
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
This PR is NOT about implementing IQv2s for header-store, but provides
IQv2s for headers store through the session state stores. So the results
do not contain the headers even though the headers are preserved in the
headers state store. Part of KIP-1271. Reviewers: Matthias Sax <mjsax@apache.org>

**Before/After**
```java
// Before
// (no removals)

// After
import org.apache.kafka.streams.state.AggregationWithHeaders;
import org.apache.kafka.streams.state.SessionStoreWithHeaders;
        } else if (store instanceof SessionStoreWithHeaders) {
            return new SessionStoreWithHeadersReadWriteDecorator<>((SessionStoreWithHeaders<?, ?>) store);
    static class SessionStoreWithHeadersReadWriteDecorator<K, AGG>
        extends SessionStoreReadWriteDecorator<K, AggregationWithHeaders<AGG>>
        implements SessionStoreWithHeaders<K, AGG> {

        SessionStoreWithHeadersReadWriteDecorator(final SessionStoreWithHeaders<K, AGG> inner) {
            super(inner);
        }
    }
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20277: add session store with headers support for iqv2 (#21670).

**Test**
Test modified in MeteredSessionStoreWithHeadersTest.java covering the corrected behaviour.
