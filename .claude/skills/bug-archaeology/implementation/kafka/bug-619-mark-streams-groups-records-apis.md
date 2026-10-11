**Commit**: 497072a564 · **Ticket**: KAFKA-19869
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
With 4.2, we are making records and APIs stable, guaranteeing no
backwards-incompatible changes.

**Before/After**
```java
// Before
        return new StreamsGroupDescribeRequest.Builder(data, true);
            new StreamsGroupHeartbeatRequest.Builder(this.heartbeatState.buildRequestData(), true),
            this(data, false);
        }

        public Builder(StreamsGroupDescribeRequestData data, boolean enableUnstableLastVersion) {
            super(ApiKeys.STREAMS_GROUP_DESCRIBE, enableUnstableLastVersion);
            this(data, false);
        }

        public Builder(StreamsGroupHeartbeatRequestData data, boolean enableUnstableLastVersion) {
            super(ApiKeys.STREAMS_GROUP_HEARTBEAT, enableUnstableLastVersion);

// After
        return new StreamsGroupDescribeRequest.Builder(data);
            new StreamsGroupHeartbeatRequest.Builder(this.heartbeatState.buildRequestData()),
            super(ApiKeys.STREAMS_GROUP_DESCRIBE);
            super(ApiKeys.STREAMS_GROUP_HEARTBEAT);
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19869: mark streams groups records and apis stable (#20841).

**Test**
Test modified in KafkaApisTest.scala covering the corrected behaviour.
