**Commit**: 55577e73b2 · **Ticket**: KAFKA-18083
**Subsystem**: test-common/src
**Category**: Resource leak

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before

                    append(socketFactoryManager.getOrCreatePortForListener(nodeId, "CONTROLLER"));
                    socketFactoryManager.getOrCreatePortForListener(node.id(), "CONTROLLER");
            formatter.setControllerListenerName("CONTROLLER");
                        getOrCreatePortForListener(controllerNode.id(), "CONTROLLER");

// After
                    append(socketFactoryManager.getOrCreatePortForListener(nodeId, controllerListenerName));
                    socketFactoryManager.getOrCreatePortForListener(node.id(), controllerListenerName);
    private final String controllerListenerName;
        this.controllerListenerName = nodes.controllerListenerName().value();
            formatter.setControllerListenerName(controllerListenerName);
                        getOrCreatePortForListener(controllerNode.id(), controllerListenerName);

    @ClusterTest(types = {Type.CO_KRAFT, Type.KRAFT}, controllerListener = "FOO")
    public void testControllerListenerName(ClusterInstance cluster) throws ExecutionException, InterruptedException {
        assertEquals("FOO", cluster.controllerListenerName().get().value());
        try (Admin admin = cluster.admin(Map.of(), true)) {
            assertEquals(1, admin.describeMetadataQuorum().quorumInfo().get().nodes().size());
```

**How to spot**
When reviewing test-common/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18083 clusterinstance custom controllerlistener not work (#17932).

**Test**
Test modified in KafkaClusterTestKit.java covering the corrected behaviour.
