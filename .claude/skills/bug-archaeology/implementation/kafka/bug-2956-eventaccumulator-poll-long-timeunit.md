**Commit**: d7bc43ed06 · **Ticket**: KAFKA-16664
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
We have revamped the thread idle ratio metric in https://github.com/apache/kafka/pull/15835. https://github.com/apache/kafka/pull/15835#discussion_r1588068337 describes a case where the metric loses accuracy and in order to set a lower bound to the accuracy, this patch re-adds a poll with a timeout that was removed as part of https://github.com/apache/kafka/pull/15430. Reviewers: David Jacot <djacot@confluent.io>

**Before/After**
```java
// Before
     * Returns the next {{@link Event}} available or null if no event is
     * available.
        lock.lock();
        try {
            K key = randomKey();
            if (key == null) return null;

            Deque<T> queue = queues.get(key);
            T event = queue.poll();

            if (queue.isEmpty()) queues.remove(key);
            inflightKeys.add(key);

// After
import java.util.concurrent.TimeUnit;
     * Immediately returns the next {{@link Event}} available or null
     * if the accumulator is empty.
        return poll(0, TimeUnit.MILLISECONDS);
     * Returns the next {{@link Event}} available. This method blocks for the provided
     * time and returns null of no event is available.
     * @param timeout   The timeout.
     * @param unit      The timeout unit.
    public T poll(long timeout, TimeUnit unit) {
            long nanos = unit.toNanos(timeout);
            while (key == null && !closed && nanos > 0) {
                    nanos = condition.awaitNanos(nanos);
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16664; re-add eventaccumulator.poll(long, timeunit) (#16144).

**Test**
Test modified in EventAccumulatorTest.java covering the corrected behaviour.
