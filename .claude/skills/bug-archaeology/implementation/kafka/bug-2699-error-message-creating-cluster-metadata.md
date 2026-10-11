**Commit**: f595802cc7 · **Ticket**: KAFKA-16975
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    /* The cluster metadata topic is an internal topic with a different implementation. The user should not be
     * allowed to create it as a regular topic.
     */
    if (topicNames.contains(Topic.CLUSTER_METADATA_TOPIC_NAME)) {
      info(s"Rejecting creation of internal topic ${Topic.CLUSTER_METADATA_TOPIC_NAME}")
    }
        if (!authorizedTopicNames.contains(name)) {
          /* The cluster metadata topic is an internal topic with a different implementation. The user should not be
           * allowed to create it as a regular topic.
           */
          if (topicNames.contains(Topic.CLUSTER_METADATA_TOPIC_NAME)) {
            info(s"Rejecting creation of internal topic ${Topic.CLUSTER_METADATA_TOPIC_NAME}")

// After
        if (name == Topic.CLUSTER_METADATA_TOPIC_NAME) {
          response.topics().add(new CreatableTopicResult().
            setName(name).
            setErrorCode(INVALID_REQUEST.code).
            setErrorMessage(s"Creation of internal topic ${Topic.CLUSTER_METADATA_TOPIC_NAME} is prohibited."))
        } else if (!authorizedTopicNames.contains(name)) {
        if (topic.name() == Topic.CLUSTER_METADATA_TOPIC_NAME) {
          topic.setErrorCode(Errors.INVALID_REQUEST.code)
          topic.setErrorMessage(s"Creation of internal topic ${Topic.CLUSTER_METADATA_TOPIC_NAME} is prohibited.")
        } else if (results.findAll(topic.name).size > 1) {
        if (!authorizedForDescribeConfigs.contains(topic.name) && topic.name() != Topic.CLUSTER_METADATA_TOPIC_NAME) {
import org.apache.kafka.common.internals.Topic
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16975 the error message of creating `__cluster_metadata` should not be "authorization failed" (#16372).

**Test**
Test modified in ControllerApisTest.scala covering the corrected behaviour.
