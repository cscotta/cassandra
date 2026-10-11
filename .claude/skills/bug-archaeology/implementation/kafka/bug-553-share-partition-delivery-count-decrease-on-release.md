**Commit**: 71299c229a · **Ticket**: KAFKA-19972

**Subsystem**: core/src/main/java/kafka/server/share/SharePartition.java

**Category**: Wrong constant/default value

**Root cause**
When a share-consumer client session was released (e.g. due to a crash) the code performed a `DECREASE` operation on the delivery count for records that had been acquired but not acknowledged. Decrementing the delivery count upon release effectively undid the increment that was made when the record was first acquired, allowing the same record to be re-delivered an unbounded number of times without ever approaching the configured maximum delivery limit. The correct operation is `NO_OP` so the count stays at the elevated level and the delivery-limit guard is respected.

**Before**
```java
// Before
return new DeliveryCountData(recordState,
    DeliveryCountOps.DECREASE);
```

**After**
```java
// After
return new DeliveryCountData(recordState,
    DeliveryCountOps.NO_OP);
```

**How to spot**
Search for `DeliveryCountOps.DECREASE` usages in `SharePartition.java` and verify whether the calling path is reached on a client-release event; if yes, and if there is no other mechanism that re-increments on the next delivery attempt, the `DECREASE` is incorrect. The pattern of "decrement on release" vs "keep on release" is the distinguishing issue.

**Test**
Write a unit test that acquires a record, releases the session without acknowledgement, re-acquires, and verifies the delivery count is greater than the initial value.
