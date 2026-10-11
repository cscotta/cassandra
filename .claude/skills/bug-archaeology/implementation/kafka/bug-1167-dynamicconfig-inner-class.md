**Commit**: a78a931ce0 · **Ticket**: KAFKA-18854
**Subsystem**: core/src
**Category**: Race condition / missing synchronization

**Root cause**
This PR is a umbrella of [KAFKA-18854. ](https://issues.apache.org/jira/browse/KAFKA-18854)

The previous PR encountered some compatibility issues, so we decided to
split it and proceed with the migration step by step. see https://github.com/apache/kafka/pull/19019

Reviewers: PoAn Yang <payang@apache.org>, Chia-Ping Tsai
 <chia7712@gmail.com>

**Before/After**
```java
// Before
import java.nio.charset.StandardCharsets
import java.util.concurrent.{ExecutionException, TimeUnit}
import java.util.{Collections, Properties}
import scala.jdk.CollectionConverters._
        val unknownConfigs = (configsToBeAdded.keys ++ configsToBeDeleted).filterNot(key => DynamicConfig.Ip.names.contains(key))
      "For entity-type '" + UserType + "': " + DynamicConfig.User.names.asScala.toSeq.sorted.map("\t" + _).mkString(nl, nl, nl) +
      "For entity-type '" + ClientType + "': " + DynamicConfig.Client.names.asScala.toSeq.sorted.map("\t" + _).mkString(nl, nl, nl) +
      "For entity-type '" + IpType + "': " + DynamicConfig.Ip.names.asScala.toSeq.sorted.map("\t" + _).mkString(nl, nl, nl) +
      "For entity-type '" + ClientMetricsType + "': " + DynamicConfig.ClientMetrics.names.asScala.toSeq.sorted.map("\t" + _).mkString(nl, nl, nl) +
      "For entity-type '" + GroupType + "': " + DynamicConfig.Group.names.asScala.toSeq.sorted.map("\t" + _).mkString(nl, nl, nl) +
import org.apache.kafka.coordinator.group.GroupConfig
  object Client {

// After
import org.apache.kafka.coordinator.group.GroupConfig
import org.apache.kafka.server.metrics.ClientMetricsConfigs
import java.nio.charset.StandardCharsets
import java.util.concurrent.{ExecutionException, TimeUnit}
import java.util.{Collections, Properties}
import scala.jdk.CollectionConverters._
        val unknownConfigs = (configsToBeAdded.keys ++ configsToBeDeleted).filterNot(key => QuotaConfig.ipConfigs.names.contains(key))
      "For entity-type '" + UserType + "': " + QuotaConfig.scramMechanismsPlusUserAndClientQuotaConfigs().names.asScala.toSeq.sorted.map("\t" + _).mkString(nl, nl, nl) +
      "For entity-type '" + ClientType + "': " + QuotaConfig.userAndClientQuotaConfigs().names.asScala.toSeq.sorted.map("\t" + _).mkString(nl, nl, nl) +
      "For entity-type '" + IpType + "': " + QuotaConfig.ipConfigs.names.asScala.toSeq.sorted.map("\t" + _).mkString(nl, nl, nl) +
      "For entity-type '" + ClientMetricsType + "': " + ClientMetricsConfigs.configDef().names.asScala.toSeq.sorted.map("\t" + _).mkString(nl, nl, nl) +
      "For entity-type '" + GroupType + "': " + GroupConfig.configDef().names.asScala.toSeq.sorted.map("\t" + _).mkString(nl, nl, nl) +
```

**How to spot**
When reviewing core/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18854 remove `dynamicconfig` inner class  (#19487).

**Test**
No test added.
