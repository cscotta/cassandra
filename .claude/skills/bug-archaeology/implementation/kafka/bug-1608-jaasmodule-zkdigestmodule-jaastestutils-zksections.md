**Commit**: 60cc2a0dea · **Ticket**: KAFKA-18556
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Mickael Maison <mickael.maison@gmail.com>

**Before/After**
```java
// Before
    public static JaasModule zkDigestModule(boolean debug, Map<String, String> entries) {
        String name = "org.apache.zookeeper.server.auth.DigestLoginModule";
        return new JaasModule(
            name,
            debug,
            entries
        );
    }

import java.util.Arrays;
    private static final String ZK_SERVER_CONTEXT_NAME = "Server";
    private static final String ZK_CLIENT_CONTEXT_NAME = "Client";

// After
    Seq(JaasTestUtils.kafkaServerSection(kafkaServerEntryName, kafkaServerSaslMechanisms.asJava, serverKeytabFile.toJava),
      JaasTestUtils.kafkaClientSection(kafkaClientSaslMechanism.toJava, clientKeytabFile.toJava))
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18556: remove jaasmodule#zkdigestmodule, jaastestutils#zksections (#18568).

**Test**
Test modified in JaasModule.java covering the corrected behaviour.
