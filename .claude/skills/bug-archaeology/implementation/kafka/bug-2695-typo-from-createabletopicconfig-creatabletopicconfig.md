**Commit**: 66655ab49a · **Ticket**: KAFKA-17095
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.common.message.CreateTopicsRequestData.CreateableTopicConfig;
                    new CreateableTopicConfig().
      { "name": "Configs", "type": "[]CreateableTopicConfig", "versions": "0+",
        CreateTopicsRequestData.CreateableTopicConfig config = creatableTopic.configs().find(CLEANUP_POLICY_CONFIG_KEY);
import org.apache.kafka.common.message.CreateTopicsRequestData.CreateableTopicConfig;
        topic2.configs().add(new CreateableTopicConfig()
import org.apache.kafka.common.message.CreateTopicsRequestData.{CreatableTopic, CreateableTopicConfig, CreateableTopicConfigCollection}

// After
import org.apache.kafka.common.message.CreateTopicsRequestData.CreatableTopicConfig;
                    new CreatableTopicConfig().
      { "name": "Configs", "type": "[]CreatableTopicConfig", "versions": "0+",
        CreateTopicsRequestData.CreatableTopicConfig config = creatableTopic.configs().find(CLEANUP_POLICY_CONFIG_KEY);
import org.apache.kafka.common.message.CreateTopicsRequestData.CreatableTopicConfig;
        topic2.configs().add(new CreatableTopicConfig()
import org.apache.kafka.common.message.CreateTopicsRequestData.{CreatableTopic, CreatableTopicConfig, CreatableTopicConfigCollection}
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17095 fix the typo from "createabletopicconfig" to "creatabletopicconfig" (#16623).

**Test**
Test modified in NewTopicTest.java covering the corrected behaviour.
