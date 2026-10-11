**Commit**: 6aef94e9ec · **Ticket**: KAFKA-18411
**Subsystem**: core/src
**Category**: Race condition / missing synchronization

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import kafka.coordinator.transaction.ZkProducerIdManager
import scala.util.{Failure, Success, Try}

    val maybeNewProducerIdsBlock = try {
      Try(ZkProducerIdManager.getNewProducerIdBlock(brokerId, zkClient, this))
    } catch {
      case ke: KafkaException => Failure(ke)
    }

    maybeNewProducerIdsBlock match {
      case Failure(exception) => callback.apply(Left(Errors.forException(exception)))
      case Success(newProducerIdBlock) => callback.apply(Right(newProducerIdBlock))

// After
import scala.util.{Failure, Try}
```

**How to spot**
When reviewing core/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18411 remove zkproduceridmanager (#18413).

**Test**
Test modified in ProducerIdManagerTest.scala covering the corrected behaviour.
