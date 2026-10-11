**Commit**: dbbb9c794a · **Ticket**: KAFKA-19959
**Subsystem**: streams/src
**Category**: Missing null / bounds check

**Root cause**
When applying the NPE fix for the `oldest-iterator-open-since-ms` metric
we only applied this to the `MeteredKeyValueStore`. This fix needs to be
applied to `MeteredSessionStore` and `MeteredWindowStore` as well. Reviewers: Matthias Sax <mjsax@apache.org>

**Before/After**
```java
// Before
                    return openIteratorsIterator.hasNext() ? openIteratorsIterator.next().startTimestamp() : null;
                (config, now) -> {
                    return openIteratorsIterator.hasNext() ? openIteratorsIterator.next().startTimestamp() : null;
            assertThat((Long) oldestIteratorTimestampMetric.metricValue(), equalTo(secondTimestamp));
        assertThat((Long) oldestIteratorTimestampMetric.metricValue(), equalTo(0L));
        assertThat(oldestIteratorTimestampMet

// After
import java.util.NoSuchElementException;
                try {
                    return openIteratorsIterator.hasNext() ? openIteratorsIterator.next().startTimestamp() : 0L;
                } catch (final NoSuchElementException e) {
                    return 0L;
            }
import java.util.NoSuchElementException;
            (config, now) -> {
                try {
                    return openIteratorsIterator.hasNext() ? openIteratorsIterator.next().startTimestamp() : 0L;
                } catch (final NoSuchElementException e) {
                    return 0L;
```

**How to spot**
When reviewing streams/src code, look for missing null / bounds check patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19959: update session and window store with new default when no open iterators (#21120).

**Test**
Test modified in MeteredKeyValueStoreTest.java covering the corrected behaviour.
