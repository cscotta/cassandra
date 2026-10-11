**Commit**: 68070c94a6 · **Ticket**: KAFKA-16724
**Subsystem**: server-common/src
**Category**: Logic error in condition

**Root cause**
Added support for fractional throughput and monotonic payload in kafka-producer-perf-test.sh. https://cwiki.apache.org/confluence/display/KAFKA/KIP-932%3A+Queues+for+Kafka#KIP932:QueuesforKafka-kafka-producer-perf-test.sh

Reviewers: Andrew Schofield <aschofield@confluent.io>, Manikumar Reddy <manikumar.reddy@gmail.com>

**Before/After**
```java
// Before
    private final long targetThroughput;
    public ThroughputThrottler(long targetThroughput, long startMs) {
                           NS_PER_SEC / targetThroughput :
        return elapsedSec > 0 && (amountSoFar / elapsedSec) > this.targetThroughput;
            int throughput = res.getInt("throughput");
                payload = generateRandomPayload(recordSize, payloadByteList, payload, random);
            SplittableRandom random) {
            throw new IllegalArgumentException("no payload File Path or record Size provided");
                .help("message size in bytes. Note that you must provide exactly one of --record-size or --payload-file.");
                        "Note that you must provide exactly one of --record-size or --payload-file.");

// After
    private final double targetThroughput;
    public ThroughputThrottler(double targetThroughput, long startMs) {
                (long) (NS_PER_SEC / targetThroughput) :
        return elapsedSec > 0 && ((double) amountSoFar / elapsedSec) > this.targetThroughput;
            double throughput = res.getDouble("throughput");
            boolean payloadMonotonic = res.getBoolean("payloadMonotonic");
                payload = generateRandomPayload(recordSize, payloadByteList, payload, random, payloadMonotonic, i);
            SplittableRandom random, boolean payloadMonotonic, long recordValue) {
        } else if (payloadMonotonic) {
            payload = Long.toString(recordValue).getBytes(StandardCharsets.UTF_8);
            throw new IllegalArgumentException("no payload File Path or record Size or payload-monotonic option provided");
                .help("message size in bytes. Note that you must provide exactly one of --record-size or --payload-file " +
```

**How to spot**
When reviewing server-common/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16724: added support for fractional throughput and monotonic payload in kafka-producer-perf-test.sh.

**Test**
Test modified in ProducerPerformanceTest.java covering the corrected behaviour.
