**Commit**: 5b8319e6c2 · **Ticket**: KAFKA-18399
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
  def handleCreateAcls(request: RequestChannel.Request): Unit = {
    metadataSupport.requireZkOrThrow(KafkaApis.shouldAlwaysForward(request))
    aclApis.handleCreateAcls(request)
  }

  def handleDeleteAcls(request: RequestChannel.Request): Unit = {
    metadataSupport.requireZkOrThrow(KafkaApis.shouldAlwaysForward(request))
    aclApis.handleDeleteAcls(request)
  }

  private def createMockRequest(): RequestChannel.Request = {
    val request: RequestChannel.Request = mock(classOf[RequestChannel.Request])

// After
// (no additions)
```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18399 remove zookeeper from kafkaapis (11/n): create_acls and delete_acls  (#18540).

**Test**
Test modified in KafkaApisTest.scala covering the corrected behaviour.
