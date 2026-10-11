**Commit**: 53c8fab814 · **Ticket**: KAFKA-19767
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
For a ShareFetchResponse containing data from multiple partitions, the
code used to add the records for each partition separately to the share
fetch buffer, signalling completion for each. This small change adds
them all in the same operation. Reviewers: Apoorv Mittal <apoorvmittal10@gmail.com>

**Before/After**
```java
// Before
                ShareCompletedFetch completedFetch = new ShareCompletedFetch(
                        requestVersion);
                shareFetchBuffer.add(completedFetch);
import java.util.function.Predicate;
    /**
     * Return whether we have any completed fetches pending return to the user. This method is thread-safe. Has
     * visibility for testing.
     *
     * @return {@code true} if there are completed fetches that match the {@link Predicate}, {@code false} otherwise
     */
    boolean hasCompletedFetches(Predicate<ShareCompletedFetch> predicate) {
        lock.lock();

// After
            List<ShareCompletedFetch> completedFetches = new ArrayList<>(responseData.size());
                completedFetches.add(
                    new ShareCompletedFetch(
                        requestVersion)
                );
            if (!completedFetches.isEmpty()) {
                shareFetchBuffer.add(completedFetches);
            }

import java.util.List;
    void add(List<ShareCompletedFetch> fetches) {
            completedFetches.addAll(fetches);
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19767: add partitions to share fetch buffer together (#20737).

**Test**
Test modified in ShareFetchBufferTest.java covering the corrected behaviour.
