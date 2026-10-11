**Commit**: 1df2ac5b2b · **Ticket**: KAFKA-19012
**Subsystem**: clients/src/main/java/org/apache/kafka/clients/producer/internals/Sender.java (and RecordAccumulator, ProducerBatch)
**Category**: Resource leak

**Root cause**
A pooled `ByteBuffer` backing a `ProducerBatch` could be returned to the `BufferPool` for reuse while the batch was still in-flight (i.e., its data was being written to the network). This happened because `RecordAccumulator.deallocate` was called for expired batches without checking whether the batch had already been handed to the `NetworkClient`. Once reused, the buffer could be overwritten with new record data, corrupting the in-flight message—misrouting records to a recently-used topic because the produce-request header (containing topic/partition) and the message body were serialized in separate buffers.

**Before/After**
```java
// Before – expired inflight batch immediately freed
for (ProducerBatch expiredBatch : expiredBatches) {
    failBatch(expiredBatch, new TimeoutException(errorMessage), false);
}

// After – inflight batches deferred; non-inflight freed immediately
failExpiredBatches(expiredBatches, now, true);           // not inflight – free now
failExpiredBatches(expiredInflightBatches, now, false);  // inflight – free later
```

**How to spot**
Any place that calls `BufferPool.deallocate` (or `RecordAccumulator.deallocate`) on a batch without first checking `batch.isInflight()` is a potential source of use-after-free. The invariant is: pooled memory must not be recycled until the network layer confirms the send is complete.

**Test**
New unit tests in `RecordAccumulatorTest` and `SenderTest` verify that `deallocate` is not called on inflight batches during expiry, and that calling it twice is logged as a warning rather than double-freeing memory.
