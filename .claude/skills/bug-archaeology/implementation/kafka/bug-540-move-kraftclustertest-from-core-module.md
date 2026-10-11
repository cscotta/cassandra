**Commit**: b825af559a · **Ticket**: KAFKA-19755
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Move KRaftClusterTest from core module to server module. Rewrite
- testCreateClusterAndCreateListDeleteTopic
- testCreateClusterAndCreateAndManyTopics
- testClientQuotas
- testDefaultClientQuotas
- testCreateClusterWithAdvertisedPortZero
- testCreateClusterWithAdvertisedHostAndPortDifferentFromSocketServer
- testUnregisterBroker

Reviewers: Hong-Yi Chen <apalan60@gmail.com>, Chia-Ping Tsai
 <chia7712@gmail.com>

**Before/After**
```java
// Before
import kafka.network.SocketServer
import org.apache.kafka.server.IntegrationTestUtils.connectAndReceive
import org.apache.kafka.common.message.DescribeClusterRequestData
import org.apache.kafka.common.quota.ClientQuotaAlteration.Op
import org.apache.kafka.common.quota.{ClientQuotaAlteration, ClientQuotaEntity, ClientQuotaFilter, ClientQuotaFilterComponent}
import org.apache.kafka.common.requests.{ApiError, DescribeClusterRequest, DescribeClusterResponse}
import org.apache.kafka.image.ClusterImage
import java.{lang, util}
import scala.concurrent.duration.{FiniteDuration, MILLISECONDS, SECONDS}
  @Test
  def testCreateClusterAndCreateListDeleteTopic(): Unit = {
    val cluster = new KafkaClusterTestKit.Builder(

// After
import org.apache.kafka.common.requests.ApiError
import java.util
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19755 move kraftclustertest from core module to server module [2/4] (#20861).

**Test**
Test modified in KRaftClusterTest.scala covering the corrected behaviour.
