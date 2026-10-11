**Commit**: 6354f49645 · **Ticket**: KAFKA-17137
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
  @ParameterizedTest
  @ValueSource(strings = Array("zk", "kraft"))
  def testDeleteTopicsWithOptionTimeoutMs(quorum: String): Unit = {
    client = createInvalidAdminClient()

    try {
      val timeoutOption = new DeleteTopicsOptions().timeoutMs(0)
      val exception = assertThrows(classOf[ExecutionException], () =>
        client.deleteTopics(Seq("test-topic").asJava, timeoutOption).all().get())
      assertInstanceOf(classOf[TimeoutException], exception.getCause)
    } finally client.close(time.Duration.ZERO)
  }
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17137 ensure admin apis are properly tested (topic-related) (#16676).

**Test**
Test modified in PlaintextAdminIntegrationTest.scala covering the corrected behaviour.
