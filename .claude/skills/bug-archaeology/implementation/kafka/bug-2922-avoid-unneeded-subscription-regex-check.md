**Commit**: 5cd6944eaa · **Ticket**: KAFKA-16493
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
This PR includes changes for AsyncKafkaConsumer to avoid evaluating the subscription regex on every poll if metadata hasn't changed. The metadataVersionSnapshot was introduced to identify whether metadata has changed or not, if yes then the current subscription regex will be evaluated. This is the same mechanism used by the LegacyConsumer. Reviewers: Lianet Magrans <lianetmr@gmail.com>, Matthias J.

**Before/After**
```java
// Before
     * TODO: remove this when we implement the KIP-848 protocol.
     *
     * The contents of this method are shamelessly stolen from
     * {@link ConsumerCoordinator#updatePatternSubscription(Cluster)} and are used here because we won't have access
     * to a {@link ConsumerCoordinator} in this code. Perhaps it could be moved to a ConsumerUtils class?
            metadata.requestUpdateForNewTopics();
                    metadata.requestUpdateForNewTopics();
        if (subscriptions.hasPatternSubscription()) {
            updatePatternSubscription(metadata.fetch());


// After
    private int metadataVersionSnapshot;
            this.metadataVersionSnapshot = metadata.updateVersion();
        this.metadataVersionSnapshot = metadata.updateVersion();
        this.metadataVersionSnapshot = metadata.updateVersion();
     *
     * This function evaluates the regex that the consumer subscribed to
     * against the list of topic names from metadata, and updates
     * the list of topics in subscription state accordingly
            this.metadataVersionSnapshot = metadata.requestUpdateForNewTopics();
                    this.metadataVersionSnapshot = metadata.requestUpdateForNewTopics();
        if (this.metadataVersionSnapshot < metadata.updateVersion()) {
            this.metadataVersionSnapshot = metadata.updateVersion();
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16493: avoid unneeded subscription regex check if metadata version unchanged (#15869).

**Test**
Test modified in AsyncKafkaConsumerTest.java covering the corrected behaviour.
