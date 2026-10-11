**Commit**: 65786fecbf · **Ticket**: KAFKA-17181
**Subsystem**: storage/src
**Category**: Non-atomic file write

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
            fileChannel.write(buffer);

// After
import org.apache.kafka.common.utils.Utils;
            Utils.writeFully(fileChannel, buffer);
```

**How to spot**
When reviewing storage/src code, look for non-atomic file write patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17181 replace filechannel.write with utils.writefully to prevent partial write (#16707).

**Test**
No test added.
