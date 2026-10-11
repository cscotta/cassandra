**Commit**: 615f1a0bf9 · **Ticket**: KAFKA-16181
**Subsystem**: core/src
**Category**: Race condition / missing synchronization

**Root cause**
This PR implement KIP-1011, kafka-configs.sh now uses incrementalAlterConfigs API to alter broker configurations instead of the deprecated alterConfigs API, and it will fall directly if the broker doesn't support incrementalAlterConfigs. Reviewers: David Jacot <djacot@confluent.io>, OmniaGM <o.g.h.ibrahim@gmail.com>.

**Before/After**
```java
// Before
import java.util.concurrent.TimeUnit
import org.apache.kafka.clients.admin.{Admin, AlterClientQuotasOptions, AlterConfigOp, AlterConfigsOptions, ConfigEntry, DescribeClusterOptions, DescribeConfigsOptions, ListTopicsOptions, ScramCredentialInfo, UserScramCredentialDeletion, UserScramCredentialUpsertion, Config => JConfig, ScramMechanism => PublicScramMechanism}
import org.apache.kafka.common.errors.InvalidConfigurationException
  @nowarn("cat=deprecation")
      case ConfigType.TOPIC =>
        alterResourceConfig(adminClient, entityTypeHead, entityNameHead, configsToBeDeleted, configsToBeAdded, ConfigResource.Type.TOPIC)

      case ConfigType.BROKER =>
        val oldConfig = getResourceConfig(adminClient, entityTypeHead, entityNameHead, includeSynonyms = false, describeAll = false)
          .map { entry => (entry.name, entry) }.toMap

        // fail the command if any of the configs to be deleted does not exist

// After
import java.util.concurrent.{ExecutionException, TimeUnit}
import org.apache.kafka.clients.admin.{Admin, AlterClientQuotasOptions, AlterConfigOp, AlterConfigsOptions, ConfigEntry, DescribeClusterOptions, DescribeConfigsOptions, ListTopicsOptions, ScramCredentialInfo, UserScramCredentialDeletion, UserScramCredentialUpsertion, ScramMechanism => PublicScramMechanism}
import org.apache.kafka.common.errors.{InvalidConfigurationException, UnsupportedVersionException}
import org.apache.kafka.common.protocol.ApiKeys

      case e: UnsupportedVersionException =>
        logger.debug(s"Unsupported API encountered in server when executing config command with args '${args.mkString(" ")}'")
        System.err.println(e.getMessage)
        Exit.exit(1)

      case ConfigType.TOPIC | ConfigType.CLIENT_METRICS | ConfigType.BROKER | ConfigType.GROUP =>
        val configResourceType = entityTypeHead match {
```

**How to spot**
When reviewing core/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16181: use incrementalalterconfigs when updating broker configs by kafka-configs.sh (#15304).

**Test**
Test modified in ConfigCommandIntegrationTest.java covering the corrected behaviour.
