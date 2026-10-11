**Commit**: c095faa578 · **Ticket**: KAFKA-18945
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
Enhance the documentation for Admin#describeCluster and
Admin#describeConfigs to clarify their behavior when using
bootstrap.controllers and bootstrap.servers. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
     * Returns a future which yields the current controller id.
     * Note that this may yield null, if the controller ID is not yet known.

// After
     * <p>
     * To obtain broker cluster information, you must configure {@link AdminClientConfig#BOOTSTRAP_SERVERS_CONFIG}.
     * To obtain controller cluster information, you must configure {@link AdminClientConfig#BOOTSTRAP_CONTROLLERS_CONFIG}.
     * Note that you cannot describe broker configs or broker logger using {@link AdminClientConfig#BOOTSTRAP_CONTROLLERS_CONFIG},
     * and you cannot describe controller configs or controller logger using {@link AdminClientConfig#BOOTSTRAP_SERVERS_CONFIG}.
     * <p>
     * Returns a future which yields the current controller node.
     * <p>
     * When using {@link AdminClientConfig#BOOTSTRAP_SERVERS_CONFIG}, the controller refer to a random broker.
     * When using {@link AdminClientConfig#BOOTSTRAP_CONTROLLERS_CONFIG}, it refers to the current voter leader.
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18945 enhance the docs for admin apis (#19315).

**Test**
No test added.
