**Commit**: a04dd21f26 · **Ticket**: KAFKA-18613
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
Implements auto-topic creation when handling the streams group
heartbeat. Inside KafkaApis, the handler for streamsGroupHeartbeat uses the result
of the streams group heartbeat inside the group coordinator to attempt
to create all missing internal topics using AutoTopicCreationManager. CREATE TOPIC ACLs are checked. The unit tests class
AutoTopicCreationManagerTest is brought back (it was recently deleted
during a ZK removal PR), but testing only the kraft-related
functionality.

**Before/After**
```java
// Before
    metadataRequestContext: Option[RequestContext]
    val request = metadataRequestContext.map { context =>
            throw new UnsupportedOperationException("Internal topic auto-creation not yet implemented.")

// After

  def createStreamsInternalTopics(
    topics: Map[String, CreatableTopic],
    requestContext: RequestContext
  ): Unit

  override def createStreamsInternalTopics(
    topics: Map[String, CreatableTopic],
    requestContext: RequestContext
  ): Unit = {

    for ((_, creatableTopic) <- topics) {
```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18613: auto-creation of internal topics in streams group heartbeat (#18981).

**Test**
Test modified in AutoTopicCreationManagerTest.scala covering the corrected behaviour.
