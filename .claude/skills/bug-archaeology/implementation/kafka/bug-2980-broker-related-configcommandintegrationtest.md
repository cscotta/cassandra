**Commit**: 21caf6b123 · **Ticket**: KAFKA-16629
**Subsystem**: checkstyle/import-control-core.xml
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import java.util.{Collections, Properties}
  def createPasswordEncoder(encoderConfigs: Map[String, String]): PasswordEncoder = {
    encoderConfigs.get(PasswordEncoderConfigs.PASSWORD_ENCODER_SECRET_CONFIG)
    val encoderSecret = encoderConfigs.getOrElse(PasswordEncoderConfigs.PASSWORD_ENCODER_SECRET_CONFIG,
      throw new IllegalArgumentException("Password encoder secret not specified"))
      encoderConfigs.getOrElse(PasswordEncoderConfigs.PASSWORD_ENCODER_CIPHER_ALGORITHM_CONFIG, PasswordEncoderConfigs.PASSWORD_ENCODER_CIPHER_ALGORITHM_DEFAULT),
      encoderConfigs.get(PasswordEncoderConfigs.PASSWORD_ENCODER_KEY_LENGTH_CONFIG).map(_.toInt).getOrElse(PasswordEncoderConfigs.PASSWORD_ENCODER_KEY_LENGTH_DEFAULT),
      encoderConfigs.get(PasswordEncoderConfigs.PASSWORD_ENCODER_ITERATIONS_CONFIG).map(_.toInt).getOrElse(PasswordEncoderConfigs.PASSWORD_ENCODER_ITERATIONS_DEFAULT))

      val passwordEncoder = createPasswordEncoder(passwordEncoderConfigs.asScala)
import org.apache.kafka.security.PasswordEncoderConfigs;
import scala.collection.JavaConverters;

// After
    <allow pkg="org.apache.kafka.clients.admin" />
import java.util.{Collections, Optional, Properties}
  def createPasswordEncoder(encoderConfigs: java.util.Map[String, String]): PasswordEncoder = {
    val encoderSecret = Optional.ofNullable(encoderConfigs.get(PasswordEncoderConfigs.PASSWORD_ENCODER_SECRET_CONFIG))
      .orElseThrow(() => new IllegalArgumentException("Password encoder secret not specified"))
      encoderConfigs.getOrDefault(PasswordEncoderConfigs.PASSWORD_ENCODER_CIPHER_ALGORITHM_CONFIG, PasswordEncoderConfigs.PASSWORD_ENCODER_CIPHER_ALGORITHM_DEFAULT),
      Optional.ofNullable(encoderConfigs.get(PasswordEncoderConfigs.PASSWORD_ENCODER_KEY_LENGTH_CONFIG))
        .map[Int](Integer.parseInt)
        .orElse(PasswordEncoderConfigs.PASSWORD_ENCODER_KEY_LENGTH_DEFAULT),
      Optional.ofNullable(encoderConfigs.get(PasswordEncoderConfigs.PASSWORD_ENCODER_ITERATIONS_CONFIG))
        .map[Int](Integer.parseInt)
        .orElse(PasswordEncoderConfigs.PASSWORD_ENCODER_ITERATIONS_DEFAULT)
```

**How to spot**
When reviewing checkstyle/import-control-core.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16629 add broker-related tests to configcommandintegrationtest (#15840).

**Test**
Test modified in ConfigCommandIntegrationTest.java covering the corrected behaviour.
