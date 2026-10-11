**Commit**: 3d14690cbf · **Ticket**: KAFKA-16790
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
n BrokerServer.scala, brokerMetadataPublishers are configured and when there are metadata updates remoteLogManager is not configured by then. Ex : remoteLogManager.foreach(rlm => rlm.onLeadershipChange(partitionsBecomeLeader.asJava, partitionsBecomeFollower.asJava, topicIds)) in ReplicaManager is invoked after publishers are instantiated, and here rlm has relevant managers configured. This change makes sure rlm is configured before the brokerMetadataPublishers initialization. Reviewers: Luke Chen <showuon@gmail.com>, Nikhil Ramakrishnan <nikrmk@amazon.com>

**Before/After**
```java
// Before
      // Start RemoteLogManager before broker start serving the requests.
      remoteLogManagerOpt.foreach { rlm =>
        val listenerName = config.remoteLogManagerConfig.remoteLogMetadataManagerListenerName()
        if (listenerName != null) {
          val endpoint = listenerInfo.listeners().values().stream
            .filter(e =>
              e.listenerName().isPresent &&
              ListenerName.normalised(e.listenerName().get()).equals(ListenerName.normalised(listenerName))
            )
            .findFirst()
            .orElseThrow(() => new ConfigException(RemoteLogManagerConfig.REMOTE_LOG_METADATA_MANAGER_LISTENER_NAME_PROP,
              listenerName, "Should be set as a listener name within valid broker listener name list: " + listenerInfo.listeners().values()))

// After
    private volatile boolean remoteLogManagerConfigured = false;

        remoteLogManagerConfigured = true;
    }

    private boolean isRemoteLogManagerConfigured() {
        return this.remoteLogManagerConfigured;
        if (this.rlmConfig.enableRemoteStorageSystem() && !isRemoteLogManagerConfigured()) {
            throw new KafkaException("RemoteLogManager is not configured when remote storage system is enabled");
        }

      // Start RemoteLogManager before initializing broker metadata publishers.
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16790: update remotelogmanager configuration in broker server (#16005).

**Test**
Test modified in RemoteLogManagerTest.java covering the corrected behaviour.
