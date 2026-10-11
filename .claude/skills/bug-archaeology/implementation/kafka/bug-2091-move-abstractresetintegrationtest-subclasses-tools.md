**Commit**: e3f953483c · **Ticket**: KAFKA-17857
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
package org.apache.kafka.streams.integration;
import org.apache.kafka.tools.StreamsResetter;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
        assertThat(resultRerun, equalTo(result));
        assertThat(resultRerun, equalTo(result));
        assertThat(resultRerun2, equalTo(result2));
                assertThat(resultIntermediate.get(i), equalTo(resultIntermediate.get(i + 11)));
            assertThat(resultIntermediate.get(10), equalTo(badMessage));

// After
    testImplementation project(':streams')
    testImplementation project(':streams').sourceSets.test.output
    testImplementation project(':streams:integration-tests').sourceSets.test.output
    testRuntimeOnly libs.hamcrest
    <allow pkg="org.apache.kafka.streams" />
package org.apache.kafka.tools;
import static org.junit.jupiter.api.Assertions.assertEquals;
        assertEquals(result, resultRerun);
        assertEquals(result, resultRerun);
        assertEquals(result2, resultRerun2);
                assertEquals(resultIntermediate.get(i + 11), resultIntermediate.get(i));
            assertEquals(badMessage, resultIntermediate.get(10));
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17857 move abstractresetintegrationtest and subclasses to tools (#17594).

**Test**
Test modified in AbstractResetIntegrationTest.java covering the corrected behaviour.
