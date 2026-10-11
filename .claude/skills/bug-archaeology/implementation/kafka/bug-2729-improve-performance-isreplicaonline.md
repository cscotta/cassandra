**Commit**: 01cf24a1ca · **Ticket**: KAFKA-17061
**Subsystem**: core/src
**Category**: State not cleaned up

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
      else liveBrokerIds.contains(brokerId)
              currentLeader == LeaderAndIsr.NoLeader || !controllerContext.liveBrokerIds.contains(currentLeader)

// After
  // To just check if a broker is live, we should use this method instead of liveBrokerIds.contains(brokerId)
  // which is more expensive because it constructs the set of live broker IDs.
  // See KAFKA-17061 for the details.
  def isLiveBroker(brokerId: Int): Boolean = liveBrokerEpochs.contains(brokerId) && !shuttingDownBrokerIds(brokerId)
      else isLiveBroker(brokerId)
              currentLeader == LeaderAndIsr.NoLeader || !controllerContext.isLiveBroker(currentLeader)
```

**How to spot**
When reviewing core/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17061 improve the performance of isreplicaonline (#16529).

**Test**
No test added.
