**Commit**: b347f4bd2e · **Ticket**: KAFKA-20297
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Summary: Move ImplicitLinkedHash*, ByteUtils from common.utils to
common.utils.internals. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.common.utils.ByteUtils;
import org.apache.kafka.common.utils.ByteUtils;
import org.apache.kafka.common.utils.ByteUtils;
import org.apache.kafka.common.utils.ByteUtils;
import org.apache.kafka.common.utils.ByteUtils;
import org.apache.kafka.common.utils.ByteUtils;
import org.apache.kafka.common.utils.ByteUtils;
import org.apache.

// After
import org.apache.kafka.common.utils.internals.ByteUtils;
import org.apache.kafka.common.utils.internals.ByteUtils;
import org.apache.kafka.common.utils.internals.ByteUtils;
import org.apache.kafka.common.utils.internals.ByteUtils;
import org.apache.kafka.common.utils.internals.ByteUtils;
import org.apache.kafka.common.utils.internals.ByteUtils;
import org.apache.kafka.common.utils.internals.ByteUtils;
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20297 move implicitlinkedhashcollection, implicitlinkedhashmulticollection, and byteutils from utils to internals .

**Test**
Test modified in SimpleExampleMessageTest.java covering the corrected behaviour.
