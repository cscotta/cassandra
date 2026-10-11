**Commit**: c91243a4b7 · **Ticket**: KAFKA-17877
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
We should only call once `maybeSendResponseCallback` for each marker during the WriteTxnMarkersRequest handling. Consider the following 2 cases:

First
We have 2 markers to append, one for producer-0, one for producer-1
When we first process producer-0, it appends a marker to the __consumer_offset. The __consumer_offset append finishes very fast because the group coordinator is no longer the leader. So the coordinator directly returns NOT_LEADER_OR_FOLLOWER.

**Before/After**
```java
// Before
        def maybeComplete(): Unit = {
          if (partitionsWithCompatibleMessageFormat.size == markerResults.size) {
              markerResults.put(partition, error)
              maybeComplete()
        replicaManager.appendRecords(
          timeout = config.requestTimeoutMs.toLong,
          requiredAcks = -1,
          internalTopicsAllowed = true,
          origin = AppendOrigin.COORDINATOR,
          entriesPerPartition = controlRecords,
          requestLocal = requestLocal,
          responseCallback = errors => {

// After
        val numPartitions = new AtomicInteger(partitionsWithCompatibleMessageFormat.size)
        def addResultAndMaybeComplete(partition: TopicPartition, error: Errors): Unit = {
          markerResults.put(partition, error)
          // We should only call maybeSendResponseCallback once per marker. Otherwise, it causes sending the response
          // prematurely.
          if (numPartitions.decrementAndGet() == 0) {
              addResultAndMaybeComplete(partition, error)
        if (controlRecords.nonEmpty) {
          replicaManager.appendRecords(
            timeout = config.requestTimeoutMs.toLong,
            requiredAcks = -1,
            internalTopicsAllowed = true,
```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17877; only call once maybesendresponsecallback for each marker (#17619).

**Test**
Test modified in KafkaApisTest.scala covering the corrected behaviour.
