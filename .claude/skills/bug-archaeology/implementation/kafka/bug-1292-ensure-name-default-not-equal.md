**Commit**: e9ffe0ba7c · **Ticket**: KAFKA-18808
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
see discussion in
[KAFKA-18735](https://issues.apache.org/jira/browse/KAFKA-18735) - the
test should include following check. 1. Using name=<default> does not create default quota
2. the returned entity should have name=<default>
2.

**Before/After**
```java
// Before
import org.apache.kafka.common.quota.{ClientQuotaAlteration, ClientQuotaEntity, ClientQuotaFilter}

// After
import org.apache.kafka.common.quota.{ClientQuotaAlteration, ClientQuotaEntity, ClientQuotaFilter, ClientQuotaFilterComponent}
  @Test
  def testDefaultNameQuotaIsNotEqualToDefaultQuota(): Unit = {
    val config = createConfig
    val defaultQuota = "<default>"
    client = Admin.create(config)

    //"<default>" can not create default quota
    val userEntity = new ClientQuotaEntity(Map(ClientQuotaEntity.USER -> defaultQuota).asJava)
    val clientEntity = new ClientQuotaEntity(Map(ClientQuotaEntity.CLIENT_ID -> defaultQuota).asJava)
    val userAlterations = new ClientQuotaAlteration(userEntity,
      Collections.singleton(new ClientQuotaAlteration.Op("consumer_byte_rate", 10000D)))
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18808 add test to ensure the name=<default> is not equal to default quota (#18966).

**Test**
Test modified in PlaintextAdminIntegrationTest.scala covering the corrected behaviour.
