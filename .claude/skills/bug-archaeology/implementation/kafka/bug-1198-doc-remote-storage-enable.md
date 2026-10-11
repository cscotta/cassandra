**Commit**: 4cdd4b617c · **Ticket**: KAFKA-19071
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
As of 3.9, Kafka allows disabling remote storage on a topic after it was
enabled. It allows subsequent enabling and disabling too. However the documentation says otherwise and needs to be corrected. Doc:
https://kafka.apache.org/39/documentation/#topicconfigs_remote.storage.enable

Reviewers: Luke Chen <showuon@gmail.com>, PoAn Yang <payang@apache.org>, Ken Huang <s7133700@gmail.com>

**Before/After**
```java
// Before
    public static final String REMOTE_LOG_STORAGE_ENABLE_DOC = "To enable tiered storage for a topic, set this configuration as true. " +
            "You can not disable this config once it is enabled. It will be provided in future versions.";
      () => admin.incrementalAlterConfigs(configs).all().get(), "Disabling remote storage feature on the topic level is not supported.")

// After
    public static final String REMOTE_LOG_STORAGE_ENABLE_DOC = "To enable tiered storage for a topic, set this configuration to true. " +
            "To disable tiered storage for a topic that has it enabled, set this configuration to false. " +
            "When disabling, you must also set <code>remote.log.delete.on.disable</code> to true.";
      () => admin.incrementalAlterConfigs(configs).all().get(), "It is invalid to disable remote storage without deleting remote data. " +
        "If you want to keep the remote data and turn to read only, please set `remote.storage.enable=true,remote.log.copy.disable=true`. " +
        "If you want to disable remote storage and delete all remote data, please set `remote.storage.enable=false,remote.log.delete.on.disable=true`.")
  }

  @ParameterizedTest
  @ValueSource(strings = Array("kraft"))
  def testUpdateTopicConfigWithDisablingRemoteStorageWithDeleteOnDisable(quorum: String): Unit = {
    val admin = createAdminClient()
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19071: fix doc for remote.storage.enable (#19345).

**Test**
Test modified in RemoteTopicCrudTest.scala covering the corrected behaviour.
