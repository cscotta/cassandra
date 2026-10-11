**Commit**: e7c17a0fb5 · **Ticket**: KAFKA-20222
**Subsystem**: streams/src
**Category**: Incorrect filtering / wrong query result

**Root cause**
Adding support for DSL integration with SessionStores with header in
support of KIP-1285. Also adds a ReadOnlySessionStoreFacade that wraps
SessionStoreWithHeaders and strips the AggregationWithHeaders wrapper
when queried through Interactive Queries, mirroring the existing pattern
for ReadOnlyKeyValueStoreFacade and ReadOnlyWindowStoreFacade. Reviewers: Alieh Saeedi <asaeedi@confluent.io>, Matthias J. Sax
 <matthias@confluent.io>

**Before/After**
```java
// Before
                assertThat(partitionResult.getFailureMessage(), is(
                    "This store"
                        + " (class org.apache.kafka.streams.state.internals.MeteredSessionStore)"
                        + " doesn't know how to execute the given query"
                        + " (WindowRangeQuery{key=Optional.empty, timeFrom=Optional[1970-01-01T00:00:00Z], timeTo=Optional[1970-01-01T00:00:00Z]})"
                        + " because SessionStores only support WindowRangeQuery.withKey."
                        + " Contact the store maintainer if you need support for a new query type."
                ));
                assertThat(partitionResult.getFailureMessage(), is(
                    "This store"
                        + " (class org.apache.kafka.streams.state.internals.MeteredSessionStore)"
                        + " doesn't know how to execute the given query"

// After
import static org.hamcrest.Matchers.containsString;
                assertThat(partitionResult.getFailureMessage(),
                    containsString("doesn't know how to execute the given query"));
                assertThat(partitionResult.getFailureMessage(),
                    containsString("because SessionStores only support WindowRangeQuery.withKey."));
                assertThat(partitionResult.getFailureMessage(),
                    containsString("doesn't know how to execute the given query"));
                assertThat(partitionResult.getFailureMessage(),
                    containsString("because SessionStores only support WindowRangeQuery.withKey."));
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
```

**How to spot**
When reviewing streams/src code, look for incorrect filtering / wrong query result patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20222: enable sessionstore with headers in dsl  (#21600).

**Test**
Test modified in IQv2StoreIntegrationTest.java covering the corrected behaviour.
