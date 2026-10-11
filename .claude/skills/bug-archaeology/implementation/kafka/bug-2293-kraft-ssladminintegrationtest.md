**Commit**: b480135b4f · **Ticket**: KAFKA-16974
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    val configs = TestUtils.createBrokerConfigs(brokerCount, zkConnectOrNull)
  @ValueSource(strings = Array("zk", "kraft"))
  @ValueSource(strings = Array("zk", "kraft"))
  @ValueSource(strings = Array("zk", "kraft"))
  @ValueSource(strings = Array("zk", "kraft"))
  @ValueSource(strings = Array("zk", "kraft"))
import org.apache.kafka.common.requests.{DeleteRecordsRequest, MetadataResponse}
import org.apache.kafka.server.config.{QuotaConfigs, ServerConfigs, ServerLogConfigs, ZkConfigs}
  @ValueSource(strings = Array("zk", "kraft"))
  @ValueSource(strings = Array("zk", "kraft"))
  @ValueSource(strings = Array("zk", "kraft"))

// After
    val configs = TestUtils.createBrokerConfigs(brokerCount, null)
  @ValueSource(strings = Array("kraft"))
  @ValueSource(strings = Array("kraft"))
  @ValueSource(strings = Array("kraft"))
  @ValueSource(strings = Array("kraft"))
  @ValueSource(strings = Array("kraft"))
import org.apache.kafka.common.requests.{DeleteRecordsRequest}
import org.apache.kafka.server.config.{QuotaConfigs, ServerConfigs, ServerLogConfigs}
  @ValueSource(strings = Array("kraft"))
  @ValueSource(strings = Array("kraft"))
  @ValueSource(strings = Array("kraft"))
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16974 kraft support in ssladminintegrationtest (#17251).

**Test**
Test modified in AdminClientWithPoliciesIntegrationTest.scala covering the corrected behaviour.
