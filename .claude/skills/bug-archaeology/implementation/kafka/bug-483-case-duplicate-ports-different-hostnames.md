**Commit**: f6136a17dd · **Ticket**: KAFKA-20080
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
### Description
As described in
[KAFKA-20080](https://issues.apache.org/jira/browse/KAFKA-20080), the
configuration "PLAINTEXT://host1:9092,SSL://host2:9092" should fail
validation due to duplicate ports, but we currently lack a unit test for
this specific case. This PR adds the missing test case to `KafkaConfigTest`. ### Verification
Ran the specific test using Gradle:
`./gradlew :core:test --tests
"kafka.server.KafkaConfigTest.testDuplicateListeners"`

**Output:**
```
Gradle Test Run :core:test > Gradle Test Executor 3 > KafkaConfigTest > testDuplicateListeners() PASSED
```

Reviewers: Ken Huang <s7133700@gmail.com>, Chia-Ping Tsai
<chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
    // listeners with duplicate port but different hostnames
    props.setProperty(SocketServerConfigs.LISTENERS_CONFIG, "PLAINTEXT://host1:9092,SSL://host2:9092")
    assertBadConfigContainingMessage(props, "Each listener must have a different port")

```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20080 add test case for duplicate ports with different hostnames in kafkaconfigtest (#21356).

**Test**
Test modified in KafkaConfigTest.scala covering the corrected behaviour.
