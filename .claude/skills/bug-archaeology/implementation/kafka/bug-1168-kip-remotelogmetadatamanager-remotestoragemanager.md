**Commit**: 6e4e0df057 · **Ticket**: KAFKA-18891
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
1. Remove `RemoteLogManager#startup` and
`RemoteLogManager#onEndpointCreated`
2. Move endpoint creation to `BrokerServer`
3. Move `RemoteLogMetadataManager#configure` and
`RemoteLogStorageManager#configure` to RemoteLogManager constructor

Reviewers: Mickael Maison <mickael.maison@gmail.com>, Ken Huang
 <s7133700@gmail.com>, Jhen-Yung Hsu <jhenyunghsu@gmail.com>

**Before/After**
```java
// Before
      remoteLogManagerOpt = createRemoteLogManager()

      // Start RemoteLogManager before initializing broker metadata publishers.
      remoteLogManagerOpt.foreach { rlm =>
        val listenerName = config.remoteLogManagerConfig.remoteLogMetadataManagerListenerName()
        if (listenerName != null) {
          val endpoint = listenerInfo.listeners().values().stream
            .filter(e =>
              e.listenerName().isPresent &&
                ListenerName.normalised(e.listenerName().get()).equals(ListenerName.normalised(listenerName))
            )
            .findFirst()

// After
    implementation project(':storage:storage-api')
    testImplementation project(':storage:storage-api').sourceSets.test.output
      remoteLogManagerOpt = createRemoteLogManager(listenerInfo)

  protected def createRemoteLogManager(listenerInfo: ListenerInfo): Option[RemoteLogManager] = {
    if (config.remoteLogManagerConfig.isRemoteStorageSystemEnabled) {
      val listenerName = config.remoteLogManagerConfig.remoteLogMetadataManagerListenerName()
      val endpoint = if (listenerName != null) {
        Some(listenerInfo.listeners().values().stream
          .filter(e =>
            e.listenerName().isPresent &&
              ListenerName.normalised(e.listenerName().get()).equals(ListenerName.normalised(listenerName))
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18891: add kip-877 support to remotelogmetadatamanager and remotestoragemanager (#19286).

**Test**
Test modified in UnifiedLogTest.scala covering the corrected behaviour.
