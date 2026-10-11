**Commit**: c92f16a727 · **Ticket**: KAFKA-17787
**Subsystem**: core/src
**Category**: Race condition / missing synchronization

**Root cause**
Reviewers: Mickael Maison <mickael.maison@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>
Co-authored-by: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import java.util.{Collections, Optional, Properties}
import kafka.server.{DynamicBrokerConfig, DynamicConfig, KafkaConfig}
import kafka.zk.{AdminZkClient, KafkaZkClient}
import org.apache.kafka.common.config.types.Password
import org.apache.kafka.common.security.JaasUtils
import org.apache.kafka.common.security.scram.internals.{ScramCredentialUtils, ScramFormatter, ScramMechanism}
import org.apache.kafka.common.utils.{Exit, Sanitizer, Time, Utils}
import org.apache.kafka.server.config.{ConfigType, QuotaConfig, ZkConfigs, ZooKeeperInternals}
import org.apache.kafka.security.{PasswordEncoder, PasswordEncoderConfigs}
import org.apache.zookeeper.client.ZKClientConfig

 *

// After
import java.util.{Collections, Properties}
import kafka.server.DynamicConfig
import org.apache.kafka.common.security.scram.internals.ScramMechanism
import org.apache.kafka.common.utils.{Exit, Utils}
import org.apache.kafka.server.config.{ConfigType, QuotaConfig}
  private val BrokerDefaultEntityName = ""
      processCommand(opts)
```

**How to spot**
When reviewing core/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17787: removed --zookeeper option and logic from configcommand (#17507).

**Test**
Test modified in ConfigCommandTest.java covering the corrected behaviour.
