**Commit**: 10c789416c · **Ticket**: KAFKA-17619
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Colin P. McCabe <cmccabe@apache.org>, Chia-Ping Tsai <chia7712@gmail.com>, David Arthur <mumrah@gmail.com>

**Before/After**
```java
// Before
import kafka.security.authorizer.AclAuthorizer;
import kafka.test.annotation.ClusterTests;
import kafka.test.junit.ZkClusterInvocationContext;
@ClusterTestDefaults(serverProperties = {
        @ClusterConfigProperty(key = StandardAuthorizer.SUPER_USERS_CONFIG, value = "User:ANONYMOUS"),
        @ClusterConfigProperty(key = AUTHORIZER_CLASS_NAME_CONFIG, value = AclCommandTest.ACL_AUTHORIZER)
})
    private static final String STANDARD_AUTHORIZER = "org.apache.kafka.metadata.authorizer.StandardAuthorizer";
    @ClusterTest(types = {Type.ZK})
    public void testAclCliWithAuthorizer(ClusterInstance cluster) throws InterruptedException {
        testAclCli(cluster, zkArgs(cluster));
    }

// After
@ClusterTestDefaults(
        types = {Type.KRAFT},
        serverProperties = {
            @ClusterConfigProperty(key = StandardAuthorizer.SUPER_USERS_CONFIG, value = "User:ANONYMOUS"),
            @ClusterConfigProperty(key = AUTHORIZER_CLASS_NAME_CONFIG, value = AclCommandTest.STANDARD_AUTHORIZER)}

)
    public static final String STANDARD_AUTHORIZER = "org.apache.kafka.metadata.authorizer.StandardAuthorizer";
    @ClusterTest
    @ClusterTest
    @ClusterTest
    @ClusterTest
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17619: remove zk type and instance from clustertest (#17284).

**Test**
Test modified in AclCommandTest.java covering the corrected behaviour.
