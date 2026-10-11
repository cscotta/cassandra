**Commit**: 73fe45f43a · **Ticket**: KAFKA-17137
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
import org.apache.kafka.common.quota.{ClientQuotaAlteration, ClientQuotaEntity, ClientQuotaFilter}
  @ParameterizedTest
  @ValueSource(strings = Array("zk", "kraft"))
  def testCreatePartitionWithOptionRetryOnQuotaViolation(quorum: String): Unit = {
    // Since it's hard to stably reach quota limit in integration test, we only verify quota configs are set correctly
    val config = createConfig
    val clientId = "test-client-id"

    config.put(AdminClientConfig.CLIENT_ID_CONFIG, clientId)
    client = Admin.create(config)

    val entity = new ClientQuotaEntity(Map(ClientQuotaEntity.CLIENT_ID -> clientId).asJava)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17137 add integration tests for admin client (createpartitions and quota) (#16687).

**Test**
Test modified in PlaintextAdminIntegrationTest.scala covering the corrected behaviour.
