**Commit**: d0653378bf · **Ticket**: KAFKA-17707
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
  // * ZooKeeper and the classic group protocol
      Arguments.of("zk", "classic"),
  def getTestQuorumAndGroupProtocolParametersZkOnly() : java.util.stream.Stream[Arguments] = {
    stream.Stream.of(
      Arguments.of("zk", "classic")
    )
  }

  // * ZooKeeper and the classic group protocol
      Arguments.of("zk", "classic"),
import org.apache.kafka.common.PartitionInfo
import scala.collection.mutable

// After
// (no additions)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17707 remove zk from baseconsumertest (#17383).

**Test**
Test modified in BaseConsumerTest.scala covering the corrected behaviour.
