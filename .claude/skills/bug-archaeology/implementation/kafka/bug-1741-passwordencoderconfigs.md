**Commit**: 3161115ada · **Ticket**: KAFKA-18361
**Subsystem**: core/src
**Category**: Wrong serialization / deserialization

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
  private val dynamicConfigPasswordEncoder = if (kafkaConfig.processRoles.isEmpty) {
    maybeCreatePasswordEncoder(kafkaConfig.passwordEncoderSecret)
  } else {
    Some(PasswordEncoder.NOOP)
  }
  private def maybeCreatePasswordEncoder(secret: Option[Password]): Option[PasswordEncoder] = {
   secret.map { secret =>
     PasswordEncoder.encrypting(secret,
        kafkaConfig.passwordEncoderKeyFactoryAlgorithm,
        kafkaConfig.passwordEncoderCipherAlgorithm,
        kafkaConfig.passwordEncoderKeyLength,
        kafkaConfig.passwordEncoderIterations)

// After
  private val dynamicConfigPasswordEncoder = Some(PasswordEncoder.NOOP)
    persistentProps.clone().asInstanceOf[Properties]
```

**How to spot**
When reviewing core/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18361 remove passwordencoderconfigs (#18347).

**Test**
Test modified in ConfigCommandIntegrationTest.java covering the corrected behaviour.
