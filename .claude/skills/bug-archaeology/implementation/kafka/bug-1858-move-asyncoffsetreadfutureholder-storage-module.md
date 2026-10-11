**Commit**: 7591868aea · **Ticket**: KAFKA-18179
**Subsystem**: core/src
**Category**: State not cleaned up

**Root cause**
Reviewers: Christo Lolov <lolovc@amazon.com>

**Before/After**
```java
// Before
import kafka.log.AsyncOffsetReadFutureHolder;

import java.util.concurrent.{CompletableFuture, Future}

/**
 * A remote log offset read task future holder. It contains two futures:
 * 1. JobFuture - Use this future to cancel the running job.
 * 2. TaskFuture - Use this future to get the result of the job/computation.
 */
case class AsyncOffsetReadFutureHolder[T](jobFuture: Future[Void], taskFuture: CompletableFuture[T]) {

}

// After
import org.apache.kafka.storage.internals.log.AsyncOffsetReadFutureHolder;
import org.apache.kafka.storage.internals.log.AsyncOffsetReadFutureHolder
import org.apache.kafka.storage.internals.log.AsyncOffsetReadFutureHolder
import org.apache.kafka.storage.internals.log.AsyncOffsetReadFutureHolder;
import org.apache.kafka.storage.internals.log.AsyncOffsetReadFutureHolder
```

**How to spot**
When reviewing core/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18179: move asyncoffsetreadfutureholder to storage module (#18095).

**Test**
Test modified in RemoteLogOffsetReaderTest.java covering the corrected behaviour.
