**Commit**: 130af38481 · **Ticket**: KAFKA-17223
**Subsystem**: clients/src
**Category**: Race condition / missing synchronization

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
            Iterator<Call> pendingIter = pendingCalls.iterator();
            while (pendingIter.hasNext()) {
                Call call = pendingIter.next();
                    pendingIter.remove();

// After
            List<Call> toRemove = new ArrayList<>();
            // Using pendingCalls.size() to get the list size before the for-loop to avoid infinite loop.
            // If call.fail keeps adding the call to pendingCalls,
            // the loop like for (int i = 0; i < pendingCalls.size(); i++) can't stop.
            int pendingSize = pendingCalls.size();
            // pendingCalls could be modified in this loop,
            // hence using for-loop instead of iterator to avoid ConcurrentModificationException.
            for (int i = 0; i < pendingSize; i++) {
                Call call = pendingCalls.get(i);
                    toRemove.add(call);

            // Use remove instead of removeAll to avoid delete all matched elements
```

**How to spot**
When reviewing clients/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17223 retrying the call after encoutering unsupportedversionexception will cause concurrentmodificationexception (.

**Test**
Test modified in KafkaAdminClientTest.java covering the corrected behaviour.
