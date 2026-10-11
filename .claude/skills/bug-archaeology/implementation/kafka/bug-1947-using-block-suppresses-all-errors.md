**Commit**: 486f65e8c6 · **Ticket**: KAFKA-18100
**Subsystem**: core/src
**Category**: State not cleaned up

**Root cause**
https://github.com/apache/kafka/pull/15881 changed our tests to utilize `using` blocks. But these blocks don't throw any errors, so if there is a failed assertion within the block, the test will still pass. We should either check the failure using a corresponding `match` block with Success(_) and Failure(e), use `using.resource`, or use try/finally blocks to clean up resources. See https://www.scala-lang.org/api/3.0.2/scala/util/Using$.html

Co-authored-by: frankvicky <kitingiao@gmail.com>

Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    Using(createPrivilegedAdminClient()) { superuserAdminClient =>
    Using(createScramAdminClient(kafkaClientSaslMechanism, describeTokenFailPrincipal.getName, describeTokenFailPassword)) { describeTokenFailAdminClient =>
      Using(createScramAdminClient(kafkaClientSaslMechanism, otherClientPrincipal.getName, otherClientPassword)) { otherClientAdminClient =>
      Using(createConsumer(configOverrides = newConsumerConfig)) { consumer =>
    Using(createPrivilegedAdminClient()) { superuserAdminClient =>

// After
    Using.resource(createPrivilegedAdminClient()) { superuserAdminClient =>
    Using.resource(createScramAdminClient(kafkaClientSaslMechanism, describeTokenFailPrincipal.getName, describeTokenFailPassword)) { describeTokenFailAdminClient =>
      Using.resource(createScramAdminClient(kafkaClientSaslMechanism, otherClientPrincipal.getName, otherClientPassword)) { otherClientAdminClient =>
      Using.resource(createConsumer(configOverrides = newConsumerConfig)) { consumer =>
import org.apache.kafka.common.config.SaslConfigs
    val superuserLoginContext = jaasAdminLoginModule(kafkaClientSaslMechanism)
    superuserClientConfig.put(SaslConfigs.SASL_JAAS_CONFIG, superuserLoginContext)
    Using.resource(createPrivilegedAdminClient()) { superuserAdminClient =>
```

**How to spot**
When reviewing core/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18100 `using` block suppresses all errors (#17954).

**Test**
Test modified in DelegationTokenEndToEndAuthorizationWithOwnerTest.scala covering the corrected behaviour.
