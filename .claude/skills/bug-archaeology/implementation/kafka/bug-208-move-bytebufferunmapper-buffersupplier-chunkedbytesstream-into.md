**Commit**: 4ae78a36ac · **Ticket**: KAFKA-20297
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
We have exposed several classes in org.apache.kafka.common.utils as
public APIs, which can be misleading since not all classes in this
package are intended for external use. So move ByteBufferUnmapper, BufferSupplier, ChunkedBytesStream into
internal module

Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.common.utils.BufferSupplier;
import org.apache.kafka.common.utils.BufferSupplier;
import org.apache.kafka.common.utils.BufferSupplier;
import org.apache.kafka.common.utils.BufferSupplier;
import org.apache.kafka.common.utils.BufferSupplier;
import org.apache.kafka.common.utils.BufferSupplier;
import org.apache.kafka.common.utils.ChunkedBytesStream;

// After
import org.apache.kafka.common.utils.internals.BufferSupplier;
import org.apache.kafka.common.utils.internals.BufferSupplier;
import org.apache.kafka.common.utils.internals.BufferSupplier;
import org.apache.kafka.common.utils.internals.BufferSupplier;
import org.apache.kafka.common.utils.internals.BufferSupplier;
import org.apache.kafka.common.utils.internals.BufferSupplier;
import org.apache.kafka.co
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20297 move bytebufferunmapper, buffersupplier, chunkedbytesstream into internal (#22081).

**Test**
Test modified in CompletedFetchTest.java covering the corrected behaviour.
