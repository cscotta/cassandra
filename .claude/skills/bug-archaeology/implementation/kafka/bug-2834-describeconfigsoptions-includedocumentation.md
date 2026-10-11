**Commit**: 6b341e6ca7 · **Ticket**: KAFKA-16547
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: TaiJuWu <tjwu1217@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
  @ParameterizedTest
  @ValueSource(strings = Array("zk", "kraft"))
  def testIncludeDocumentation(quorum: String): Unit = {
    createTopic(topic)
    client = createAdminClient

    val resource = new ConfigResource(ConfigResource.Type.TOPIC, topic)
    val resources = Collections.singletonList(resource)
    val includeDocumentation = new DescribeConfigsOptions().includeDocumentation(true)
    var describeConfigs = client.describeConfigs(resources, includeDocumentation)
    var configEntries = describeConfigs.values().get(resource).get().entries()
    configEntries.forEach(e => assertNotNull(e.documentation()))
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16547 add test for describeconfigsoptions#includedocumentation (#16355).

**Test**
Test modified in PlaintextAdminIntegrationTest.scala covering the corrected behaviour.
