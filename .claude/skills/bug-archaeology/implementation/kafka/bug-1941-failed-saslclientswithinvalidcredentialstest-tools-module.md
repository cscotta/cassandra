**Commit**: e1ba01d214 · **Ticket**: KAFKA-18109
**Subsystem**: tools/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
import org.apache.kafka.common.config.SaslConfigs;
        String superuserLoginContext = jaasAdminLoginModule(KAFKA_CLIENT_SASL_MECHANISM, Option.empty());
        this.superuserClientConfig().put(SaslConfigs.SASL_JAAS_CONFIG, superuserLoginContext);
```

**How to spot**
When reviewing tools/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18109 fix failed saslclientswithinvalidcredentialstest in tools module (#17971).

**Test**
Test modified in SaslClientsWithInvalidCredentialsTest.java covering the corrected behaviour.
