**Commit**: 22bef988d4 · **Ticket**: KAFKA-18926
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
In KRaft, custom KafkaPrincipalBuilder instances must implement
KafkaPrincipalSerde to support the forward mechanism. Currently, this
requirement is not enforced and relies on the developer's attention. With this patch, we can prevent incorrect implementations at compile
time. Reviewers: Ken Huang <s7133700@gmail.com>, TengYao Chi
 <kitingiao@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
            return principalBuilder instanceof KafkaPrincipalSerde ? Optional.of((KafkaPrincipalSerde) principalBuilder) : Optional.empty();
            return principalBuilder instanceof KafkaPrincipalSerde ? Optional.of((KafkaPrincipalSerde) principalBuilder) : Optional.empty();
 *
 * Note that custom implementations of {@link KafkaPrincipalBuilder}
 * must also implement {@link KafkaPrincipalSerde}, otherwise brokers will not be able to
 * forward requests to the controller.
public interface KafkaPrincipalBuilder {
import org.apache.kafka.common.security.auth.KafkaPrincipalSerde;
public class DefaultKafkaPrincipalBuilder implements KafkaPrincipalBuilder, KafkaPrincipalSerde {
        return principalBuilder instanceof KafkaPrincipalSerde ? Optional.of((KafkaPrincipalSerde) principalBuilder) : Optional.empty();

// After
            return Optional.of(principalBuilder);
            return Optional.of(principalBuilder);
public interface KafkaPrincipalBuilder extends KafkaPrincipalSerde {
public class DefaultKafkaPrincipalBuilder implements KafkaPrincipalBuilder {
        return Optional.of(principalBuilder);
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18926 kafkaprincipalbuilder should extend kafkaprincipalserde (#19987).

**Test**
Test modified in ChannelBuildersTest.java covering the corrected behaviour.
