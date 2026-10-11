**Commit**: 2299a06e31 · **Ticket**: KAFKA-20297
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
We have exposed several classes in org.apache.kafka.common.utils as
public APIs, which can be misleading since not all classes in this
package are intended for external use. To clarify API boundaries and prevent unintended usage, we propose
moving the following classes to an internal package:

- AbstractIterator
- CircularIterator
- CloseableIterator
- CopyOnWriteMap
- FixedOrderMap
- PrimitiveRef

Reviewers: Christo <lolovc@amazon.com>, PoAn Yang <payang@apache.org>

**Before/After**
```java
// Before
import org.apache.kafka.common.utils.AbstractIterator;
import org.apache.kafka.common.utils.CircularIterator;
import org.apache.kafka.common.utils.CloseableIterator;
import org.apache.kafka.common.utils.CloseableIterator;
import org.apache.kafka.common.utils.CopyOnWriteMap;
import org.apache.kafka.common.utils.PrimitiveRef;

// After
import org.apache.kafka.common.utils.internals.AbstractIterator;
import org.apache.kafka.common.utils.internals.CircularIterator;
import org.apache.kafka.common.utils.internals.CloseableIterator;
import org.apache.kafka.common.utils.internals.CloseableIterator;
import org.apache.kafka.common.utils.internals.CopyOnWriteMap;
import org.apache.kafka.common.utils.internals.PrimitiveRef;
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20297: move abstractiterator, circulariterator, closeableiterator... into internal (#22052).

**Test**
Test modified in DefaultRecordBatchTest.java covering the corrected behaviour.
