**Commit**: e846491d60 · **Ticket**: KAFKA-19755
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
Move KRaftClusterTest from core module to server module. Rewrite

1. testCreateClusterAndClose
2. testCreateClusterAndRestartBrokerNode
3.

**Before/After**
```java
// Before
import org.apache.kafka.common.acl.{AclBinding, AclBindingFilter}
import org.apache.kafka.common.config.{ConfigException, ConfigResource}
import org.apache.kafka.common.security.auth.KafkaPrincipal
import org.apache.kafka.common.{Cluster, Endpoint, Reconfigurable, TopicPartition, TopicPartitionInfo}
import org.apache.kafka.server.authorizer._
import org.apache.kafka.server.config.{KRaftConfigs, ReplicationConfigs, ServerConfigs}
import org.apache.kafka.server.quota
import org.apache.kafka.server.quota.{ClientQuotaCallback, ClientQuotaType}
import java.util.concurrent.{CompletableFuture, CompletionStage, ExecutionException, TimeUnit}
import java.util.concurrent.atomic.AtomicInteger
  @Test
  def testCreateClusterAndClose(): Unit = {

// After
import org.apache.kafka.common.config.ConfigResource
import org.apache.kafka.common.{TopicPartition, TopicPartitionInfo}
import org.apache.kafka.server.config.{KRaftConfigs, ServerConfigs}
import java.util.concurrent.{ExecutionException, TimeUnit}
```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19755 move kraftclustertest from core module to server module [1/n] (#20719).

**Test**
Test modified in KRaftClusterTest.scala covering the corrected behaviour.
