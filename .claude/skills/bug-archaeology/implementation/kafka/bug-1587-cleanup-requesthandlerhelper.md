**Commit**: ce5d7be1dc · **Ticket**: KAFKA-18492
**Subsystem**: core/src
**Category**: State not cleaned up

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import kafka.cluster.Partition
import kafka.coordinator.transaction.TransactionCoordinator
import org.apache.kafka.common.internals.Topic
import org.apache.kafka.coordinator.group.GroupCoordinator
import java.util.OptionalInt

object RequestHandlerHelper {

  def onLeadershipChange(groupCoordinator: GroupCoordinator,
                         txnCoordinator: TransactionCoordinator,
                         updatedLeaders: Iterable[Partition],
                         updatedFollowers: Iterable[Partition]): Unit = {

// After
// (no additions)
```

**How to spot**
When reviewing core/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18492 cleanup requesthandlerhelper (#18608).

**Test**
No test added.
