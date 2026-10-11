**Commit**: 71d08780d1 · **Ticket**: KAFKA-14690
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
This patch extends the OffsetCommit API to support topic ids. From
version 10 of the API, topic ids must be used. Originally, we wanted to
support both using topic ids and topic names from version 10 but it
turns out that it makes everything more complicated. Hence we propose to
only support topic ids from version 10.

**Before/After**
```java
// Before
        return new OffsetCommitRequest.Builder(data);
            OffsetCommitRequest.Builder builder = new OffsetCommitRequest.Builder(data);
        OffsetCommitRequest.Builder builder = new OffsetCommitRequest.Builder(
        public Builder(OffsetCommitRequestData data, boolean enableUnstableLastVersion) {
            super(ApiKeys.OFFSET_COMMIT, enableUnstableLastVersion);
        public Builder(OffsetCommitRequestData data) {
            this(data, false);
                throw new UnsupportedVersionException("The broker offset commit protocol version " +
                        version + " does not support usage of config group.instance.id.");

// After
        return OffsetCommitRequest.Builder.forTopicNames(data);
            OffsetCommitRequest.Builder builder = OffsetCommitRequest.Builder.forTopicNames(data);
        OffsetCommitRequest.Builder builder = OffsetCommitRequest.Builder.forTopicNames(
import org.apache.kafka.common.Uuid;
        private Builder(OffsetCommitRequestData data, short oldestAllowedVersion, short latestAllowedVersion) {
            super(ApiKeys.OFFSET_COMMIT, oldestAllowedVersion, latestAllowedVersion);
        public static Builder forTopicIdsOrNames(OffsetCommitRequestData data, boolean enableUnstableLastVersion) {
            return new Builder(data, ApiKeys.OFFSET_COMMIT.oldestVersion(), ApiKeys.OFFSET_COMMIT.latestVersion(enableUnstableLastVersion));
        }

        public static Builder forTopicNames(OffsetCommitRequestData data) {
            return new Builder(data, ApiKeys.OFFSET_COMMIT.oldestVersion(), (short) 9);
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-14690; add topicid to offsetcommit api (#19461).

**Test**
Test modified in ConsumerCoordinatorTest.java covering the corrected behaviour.
