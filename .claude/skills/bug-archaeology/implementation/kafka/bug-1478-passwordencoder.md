**Commit**: 9f78771a1f · **Ticket**: KAFKA-18693
**Subsystem**: core/src
**Category**: Wrong serialization / deserialization

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.common.config.types.Password
import org.apache.kafka.security.PasswordEncoder
  private val dynamicConfigPasswordEncoder = Some(PasswordEncoder.NOOP)
  private def passwordEncoder: PasswordEncoder = {
    dynamicConfigPasswordEncoder.getOrElse(throw new ConfigException("Password encoder secret not configured"))
  }

  private[server] def toPersistentProps(configProps: Properties, perBrokerConfig: Boolean): Properties = {
    val props = configProps.clone().asInstanceOf[Properties]

    def encodePassword(configName: String, value: String): Unit = {
      if (value != null) {

// After
        config.dynamicConfig.updateBrokerConfig(0, props)
```

**How to spot**
When reviewing core/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18693 remove passwordencoder (#18790).

**Test**
Test modified in DynamicBrokerConfigTest.scala covering the corrected behaviour.
