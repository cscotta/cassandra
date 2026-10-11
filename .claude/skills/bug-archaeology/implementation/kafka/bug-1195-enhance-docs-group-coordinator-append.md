**Commit**: dced8bfb38 · **Ticket**: KAFKA-19102
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
Update the `group.coordinator.append.linger.ms` document, alterting
users to the potentail consequences of increasing value, It should
explain that increasing that value would improves write efficiency but
also increases response latency for requests. Reviewers: TengYao Chi <kitingiao@gmail.com>, Chia-Ping Tsai
 <chia7712@gmail.com>, PoAn Yang <payang@apache.org>

**Before/After**
```java
// Before
        "wait for writes to accumulate before flushing them to disk. Transactional writes are not accumulated.";

// After
        "wait for writes to accumulate before flushing them to disk. Increasing this value improves write efficiency and batch size, " +
        "but also increases the response latency for requests, as the coordinator must wait for batches to be flushed to " +
        "disk before completing request processing. Transactional writes are not accumulated.";
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19102 enhance the docs of group.coordinator.append.linger.ms (#19451).

**Test**
No test added.
