**Commit**: e4b8644400 · **Ticket**: KAFKA-17992
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: TengYao Chi <kitingiao@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
            "--entity-name", cluster.isKRaftTest() ? "0" : "1",
                raftInstance.getUnderlying().brokers().get(0).logManager()
        raftInstance.getUnderlying().brokers().get(0).shutdown();
        raftInstance.getUnderlying().brokers().get(0).startup();
    if (cluster.isKRaftTest && apiVersion >= 3) {
    val expectedApis = if (!cluster.isKRaftTest) {
      ApiVersionsResponse.collectApis(
        ApiKeys.apisForListener(ApiMessageType.ListenerType.ZK_BROKER),
        enableUnstableLastVersion
      )
    } else if (cluster.controllerListenerName().toScala.contains(listenerName)) {
    val defaultApiVersionsResponse = if (!cluster.isKRaftTest) {

// After
            "--entity-name", "0",
                raftInstance.brokers().get(0).logManager()
        raftInstance.brokers().get(0).shutdown();
        raftInstance.brokers().get(0).startup();
    if (apiVersion >= 3) {
    val expectedApis = if (cluster.controllerListenerName().toScala.contains(listenerName)) {
    val defaultApiVersionsResponse = if (cluster.controllerListenerName().toScala.contains(listenerName)) {
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17992 remove getunderlying and iskrafttest from clusterinstance (#17802).

**Test**
Test modified in ConfigCommandIntegrationTest.java covering the corrected behaviour.
