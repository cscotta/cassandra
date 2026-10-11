**Commit**: a39c984d21 · **Ticket**: KAFKA-15561
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: David Jacot <djacot@confluent.io>

**Before/After**
```java
// Before
    public void subscribe(SubscriptionPattern pattern, ConsumerRebalanceListener callback) {
        throw new UnsupportedOperationException("Subscribe to RE2/J regular expression not supported in MockConsumer yet");
        throw new UnsupportedOperationException("Subscribe to RE2/J regular expression not supported in MockConsumer yet");
    public void subscribe(SubscriptionPattern pattern, ConsumerRebalanceListener callback) {
        subscribeToRegex(pattern, Optional.ofNullable(callback));

// After
    public void subscribe(SubscriptionPattern pattern, ConsumerRebalanceListener listener) {
        if (listener == null)
            throw new IllegalArgumentException("RebalanceListener cannot be null");
        subscribe(pattern, Optional.of(listener));
        subscribe(pattern, Optional.empty());
    }

    private void subscribe(SubscriptionPattern pattern, Optional<ConsumerRebalanceListener> listener) {
        if (pattern == null || pattern.toString().isEmpty())
            throw new IllegalArgumentException("Topic pattern cannot be " + (pattern == null ? "null" : "empty"));
        ensureNotClosed();
        committed.clear();
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15561 [4/n]: mockconsumer support for subscriptionpattern (#17962).

**Test**
Test modified in MockConsumerTest.java covering the corrected behaviour.
