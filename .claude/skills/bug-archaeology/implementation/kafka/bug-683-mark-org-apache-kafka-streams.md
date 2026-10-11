**Commit**: 8dfaedd73a · **Ticket**: KAFKA-19816
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Marks

`org.apache.kafka.streams.integration.EosIntegrationTest.shouldNotViolateEosIfOneTaskFails`
as flaky  due to issues with test runs of the "streams" group protocol. Ticket to resolve: https://issues.apache.org/jira/browse/KAFKA-19816

Reviewers: Lianet Magrans <lmagrans@confluent.io>, Chia-Ping Tsai
 <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
    testImplementation project(':test-common:test-common-util')
import org.apache.kafka.common.test.api.Flaky;
    @Flaky("KAFKA-19816")
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: minor: mark`org.apache.kafka.streams.integration.eosintegrationtest.shouldnotviolateeosifonetaskfails` as flaky (#20733).

**Test**
Test modified in EosIntegrationTest.java covering the corrected behaviour.
