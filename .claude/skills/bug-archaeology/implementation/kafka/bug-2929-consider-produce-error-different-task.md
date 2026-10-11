**Commit**: 8a2bc3a221 · **Ticket**: KAFKA-16903
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
A task does not know anything about a produce error thrown
by a different task. That might lead to a InvalidTxnStateException
when a task attempts to do a transactional operation on a producer
that failed due to a different task. This commit stores the produce exception in the streams producer
on completion of a send instead of the record collector since the
record collector is on task level whereas the stream producer
is on stream thread level. Since all tasks use the same streams
producer the error should be correctly propagated across tasks
of the same stream thread.

**Before/After**
```java
// Before
    private final AtomicReference<KafkaException> sendException = new AtomicReference<>(null);


// After
    private final AtomicReference<KafkaException> sendException;
        this.sendException = streamsProducer.sendException();
import java.util.concurrent.atomic.AtomicReference;
    private final AtomicReference<KafkaException> sendException = new AtomicReference<>(null);
    AtomicReference<KafkaException> sendException() {
        return sendException;
    }

import java.util.concurrent.atomic.AtomicReference;
        when(streamsProducer.sendException()).thenReturn(new AtomicReference<>(null));
        when(streamsProducer.sendException()).thenReturn(new AtomicReference<>(null));
        when(streamsProducer.sendException()).thenReturn(new AtomicReference<>(null));
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16903: consider produce error of different task (#16222).

**Test**
Test modified in RecordCollectorTest.java covering the corrected behaviour.
