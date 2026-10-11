**Commit**: e1b2adea07 · **Ticket**: KAFKA-17190
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
In MetadataVersion 3.7-IV2 and above, the broker's AssignmentsManager sends an RPC to the
controller informing it about which directory we have chosen to place each new replica on. Unfortunately, the code does not check to see if the topic still exists in the MetadataImage before
sending the RPC. It will also retry infinitely. Therefore, after a topic is created and deleted in
rapid succession, we can get stuck including the now-defunct replica in our subsequent
AssignReplicasToDirsRequests forever.

**Before/After**
```java
// Before
        () => lifecycleManager.brokerEpoch,
        (directoryId: Uuid) => logManager.directoryPath(directoryId).asJava,
        (topicId: Uuid) => Optional.ofNullable(metadataCache.topicIdsToNames().get(topicId))

// After
    implementation project(':metadata')
    <allow pkg="org.apache.kafka.server" />
    <allow pkg="org.apache.kafka.image" />
    public long initialInterval() {
        return initialInterval;
    }

        () => metadataCache.getImage(),
        (directoryId: Uuid) => logManager.directoryPath(directoryId).
          getOrElse("[unknown directory path]")
  def getImage(): MetadataImage = {
    _currentImage
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17190: assignmentsmanager gets stuck retrying on deleted topics (#16672).

**Test**
Test modified in ClusterImageTest.java covering the corrected behaviour.
