**Commit**: e4d108d4df · **Ticket**: KAFKA-15793
**Subsystem**: .github/workflows
**Category**: Logic error in condition

**Root cause**
Reviewers: Igor Soarez <soarez@apple.com>, Ajit Singh <>

**Before/After**
```java
// Before
          ./gradlew --build-cache --scan --continue \
log4j.logger.kafka=WARN
log4j.logger.org.apache.kafka=WARN
      MetadataVersion.IBP_3_4_IV0,
@Timeout(300)
    val newTopics = new util.ArrayList[NewTopic]()
    newTopics.add(new NewTopic("test-topic-1", 10, 3.toShort))
    newTopics.add(new NewTopic("test-topic-2", 10, 3.toShort))
    newTopics.add(new NewTopic("test-topic-3", 10, 3.toShort))
    val createTopicResult = admin.createTopics(newTopics)
    createTopicResult.all().get(300, TimeUnit.SECONDS)
    admin.close()

// After
          ./gradlew --info --build-cache --scan --continue \
log4j.logger.kafka=INFO
log4j.logger.org.apache.kafka=INFO
@Timeout(600)
    try {
      val newTopics = new util.ArrayList[NewTopic]()
      newTopics.add(new NewTopic("test-topic-1", 10, 3.toShort))
      newTopics.add(new NewTopic("test-topic-2", 10, 3.toShort))
      newTopics.add(new NewTopic("test-topic-3", 10, 3.toShort))
      val createTopicResult = admin.createTopics(newTopics)
      createTopicResult.all().get(61, TimeUnit.SECONDS)
      TestUtils.waitUntilTrue(() => {
```

**How to spot**
When reviewing .github/workflows code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15793 fix zkmigrationintegrationtest#testmigratetopicdeletions (#17004).

**Test**
Test modified in log4j.properties covering the corrected behaviour.
