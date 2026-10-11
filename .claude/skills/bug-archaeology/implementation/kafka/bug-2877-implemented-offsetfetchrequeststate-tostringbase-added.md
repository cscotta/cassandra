**Commit**: e59c887bfd · **Ticket**: KAFKA-16557
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Lianet Magrans <lianetmr@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    private OffsetFetchRequestState createOffsetFetchRequest(final Set<TopicPartition> partitions,
        public String toString() {
            return "OffsetFetchRequestState{" +
                    "requestedPartitions=" + requestedPartitions +
                    ", memberId=" + memberInfo.memberId.orElse("undefined") +
                    ", memberEpoch=" + (memberInfo.memberEpoch.isPresent() ? memberInfo.memberEpoch.get() : "undefined") +
                    ", future=" + future +
                    ", " + toStringBase() +
                    '}';

// After
    // Visible for testing
    OffsetFetchRequestState createOffsetFetchRequest(final Set<TopicPartition> partitions,
        @Override
        public String toStringBase() {
            return super.toStringBase() + ", " + memberInfo;
        }

        public String toStringBase() {
            return super.toStringBase() +
                    ", requestedPartitions=" + requestedPartitions;

        @Override
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16557 implemented offsetfetchrequeststate tostringbase and added a test for it (#16291).

**Test**
Test modified in CommitRequestManagerTest.java covering the corrected behaviour.
