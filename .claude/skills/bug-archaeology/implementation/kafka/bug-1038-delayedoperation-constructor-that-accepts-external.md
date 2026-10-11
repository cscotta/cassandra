**Commit**: 6e380fbbcc · **Ticket**: KAFKA-19322
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Remove the DelayedOperation constructor that accepts an external lock. According to this [PR](https://github.com/apache/kafka/pull/19759). Reviewers: Ken Huang <s7133700@gmail.com>, PoAn Yang
 <payang@apache.org>, TengYao Chi <kitingiao@gmail.com>, Chia-Ping Tsai
 <chia7712@gmail.com>

**Before/After**
```java
// Before
        super(shareFetch.fetchParams().maxWaitMs, Optional.empty());
import java.util.concurrent.locks.Lock
import scala.jdk.OptionConverters.RichOption
                     responseCallback: Map[TopicIdPartition, PartitionResponse] => Unit,
                     lockOpt: Option[Lock])
  extends DelayedOperation(delayMs, lockOpt.toJava) with Logging {
import java.util.concurrent.locks.Lock
   * @param delayedProduceLock            lock for the delayed actions
                    delayedProduceLock: Option[Lock] = None,
      delayedProduceLock,
    delayedProduceLock: Option[Lock],
      val delayedProduce = new DelayedProduce(timeoutMs, produceMetadata, this, responseCallback, delayedProduceLock)

// After
        super(shareFetch.fetchParams().maxWaitMs);
                     responseCallback: Map[TopicIdPartition, PartitionResponse] => Unit)
  extends DelayedOperation(delayMs) with Logging {
      val delayedProduce = new DelayedProduce(timeoutMs, produceMetadata, this, responseCallback)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19322 remove the delayedoperation constructor that accepts an external lock (#19798).

**Test**
Test modified in AbstractCoordinatorConcurrencyTest.scala covering the corrected behaviour.
