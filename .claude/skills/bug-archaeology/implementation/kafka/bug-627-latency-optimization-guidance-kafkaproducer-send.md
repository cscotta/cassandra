**Commit**: 58d439a1aa · **Ticket**: KAFKA-14768
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Background: There have been many similar issues discussed about send
latency (block for first sent) in JIRA and KIPs. After diving deep into
the code and weighing  the effort versus the reward, I think the most
suitable approach might  be to simply add a guideline in the JavaDoc to
help more people become  aware of it and understand possible solutions
so that many persons can reduce work load for deep dive this similar
issue. FYI:  The similar JIRAs https://issues.apache.org/jira/browse/KAFKA-6705
https://issues.apache.org/jira/browse/KAFKA-3539
https://issues.apache.org/jira/browse/KAFKA-14768

Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
     * the cluster by this client for the given topic. In this case it will block for up to {@code max.block.ms} milliseconds if
     * Kafka cluster is unreachable; 2) Allocating a buffer if buffer pool doesn't have any free buffers.

// After
     * the cluster by this client for the given topic. In this case it will block for up to {@code max.block.ms} milliseconds
     * while waiting for topic's metadata if Kafka cluster is unreachable; 2) Allocating a buffer if buffer pool doesn't
     * have any free buffers.
     * <p>
     * <b>Reducing first-send latency:</b> You can reduce the latency of the first send by preloading the metadata
     * with {@link #partitionsFor(String)}. However, be aware that metadata cache will be cleared to free up resources after
     * {@code metadata.max.idle.ms} of inactivity, so subsequent sends after a long idle period will still
     * experience delays.
     * @see #partitionsFor(String)
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-14768 add latency optimization guidance to kafkaproducer.send() (#20842).

**Test**
No test added.
