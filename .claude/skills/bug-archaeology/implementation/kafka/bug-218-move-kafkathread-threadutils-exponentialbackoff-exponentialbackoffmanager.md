**Commit**: b4c977544f · **Ticket**: KAFKA-20297
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Summary: move `KafkaThread`, `ThreadUtils`, `ExponentialBackoff` and
`ExponentialBackoffManager` from utils to utils.internal

Reviewers: Christo Lolov <lolovc@amazon.com>, Ken Huang
 <s7133700@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.common.utils.ExponentialBackoff;
import org.apache.kafka.common.utils.ExponentialBackoff;
import org.apache.kafka.common.utils.ExponentialBackoff;
import org.apache.kafka.common.utils.KafkaThread;
import org.apache.kafka.common.utils.ExponentialBackoff;
import org.apache.kafka.common.utils.ExponentialBackoff;
import org.apache.kafka.common.utils.KafkaThread;

// After
import org.apache.kafka.common.utils.internals.ExponentialBackoff;
import org.apache.kafka.common.utils.internals.ExponentialBackoff;
import org.apache.kafka.common.utils.internals.ExponentialBackoff;
import org.apache.kafka.common.utils.internals.KafkaThread;
import org.apache.kafka.common.utils.internals.ExponentialBackoff;
import org.apache.kafka.common.utils.internals.ExponentialBackoff;
import org.apache.kafka.common.utils.internals.KafkaThread;
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20297 move kafkathread, threadutils, exponentialbackoff and exponentialbackoffmanager to internal (#22049).

**Test**
Test modified in ExponentialBackoffManagerTest.java covering the corrected behaviour.
