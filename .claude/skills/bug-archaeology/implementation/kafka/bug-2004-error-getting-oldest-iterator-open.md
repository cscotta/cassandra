**Commit**: 57299cfbb1 · **Ticket**: KAFKA-17954
**Subsystem**: streams/src
**Category**: Race condition / missing synchronization

**Root cause**
The thread that evaluates the gauge for the oldest-iterator-open-since-ms runs concurrently
with threads that open/close iterators (stream threads and interactive query threads). This PR
fixed a race condition between `openIterators.isEmpty()` and `openIterators.first()`, by catching
a potential exception. Because we except the race condition to be rare, we rather catch the
exception in favor of introducing a guard via locking. Reviewers: Matthias J.

**Before/After**
```java
// Before
                (config, now) -> openIterators.isEmpty() ? null : openIterators.first().startTimestamp()
                (config, now) -> openIterators.isEmpty() ? null : openIterators.first().startTimestamp()
                (config, now) -> openIterators.isEmpty() ? null : openIterators.first().startTimestamp()

// After
import java.util.NoSuchElementException;
                (config, now) -> {
                    try {
                        return openIterators.isEmpty() ? null : openIterators.first().startTimestamp();
                    } catch (final NoSuchElementException ignored) {
                        return null;
                    }
                }
import java.util.NoSuchElementException;
                (config, now) -> {
                    try {
                        return openIterators.isEmpty() ? null : openIterators.first().startTimestamp();
```

**How to spot**
When reviewing streams/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17954: error getting oldest-iterator-open-since-ms from jmx (#17713).

**Test**
No test added.
