**Commit**: 7aaba96cc1 · **Ticket**: KAFKA-19282
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
In `kafka.server.ClientQuotaManager` class, `quotaTypesEnabled` is not updated when a quota is removed via `removeQuota` method in `DefaultQuotaCallback` class. This field is set when quotas are added in `updateQuota` but it's never changed or cleared. So in case all the quotas have been removed dynamically, the system may incorrectly assume the quotas are active, which leads to unnecessary metric creation or updates until the broker is restarted. Reviewers: Jonah Hooper <jhooper@confluent.io>, Hailey Ni <hni@confluent.io>, Alyssa Huang <ahuang@confluent.io>, David Jacot <djacot@confluent.io>, Rajini Sivaram <rajinisivaram@googlemail.com>

**Before/After**
```java
// Before
  private var quotaTypesEnabled = clientQuotaCallbackPlugin match {
   * Note: If any quotas (static defaults, dynamic defaults or quota overrides) have
   * been configured for this broker at any time for this quota type, quotasEnabled will
   * return true until the next broker restart, even if all quotas are subsequently deleted.
      if (userEntity.nonEmpty) {
        if (quotaEntity.clientIdEntity.nonEmpty)
          quotaTypesEnabled |= QuotaTypes.UserClientIdQuotaEnabled
        else
          quotaTypesEnabled |= QuotaTypes.UserQuotaEnabled
      } else if (clientEntity.nonEmpty)
        quotaTypesEnabled |= QuotaTypes.ClientIdQuotaEnabled


// After
  private val activeQuotaEntities = new ConcurrentHashMap[Int, Int]() // Key is QuotaTypes, value is the number of active quota entities of that type


  var quotaTypesEnabled = clientQuotaCallbackPlugin match {
        case Some(newQuota) =>
          updateQuotaTypes(quotaEntity, shouldAdd = true)
          quotaCallback.updateQuota(clientQuotaType, quotaEntity, newQuota.bound)

        case None =>
          updateQuotaTypes(quotaEntity, shouldAdd = false)
          quotaCallback.removeQuota(clientQuotaType, quotaEntity)

```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19282: update quotatypesenabled on quota removal in clientquotamanager (#19742).

**Test**
Test modified in ClientQuotaManagerTest.scala covering the corrected behaviour.
