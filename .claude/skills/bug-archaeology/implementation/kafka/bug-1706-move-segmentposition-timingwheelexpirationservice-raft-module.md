**Commit**: d1aa370db8 · **Ticket**: KAFKA-15599
**Subsystem**: core/src
**Category**: State not cleaned up

**Root cause**
Reviewers: Divij Vaidya <diviv@amazon.com>, Jason Taylor <jastaylr@amazon.com>

**Before/After**
```java
// Before
import org.apache.kafka.raft.{Isolation, KafkaRaftClient, LogAppendInfo, LogFetchInfo, LogOffsetMetadata, OffsetAndEpoch, OffsetMetadata, ReplicatedLog, ValidOffsetAndEpoch}
        Optional.of(SegmentPosition(
      Optional.of(SegmentPosition(
      Optional.of(SegmentPosition(hwm.segmentBaseOffset, hwm.relativePositionInSegment))
import org.apache.kafka.raft.{Endpoints, FileQuorumStateStore, KafkaNetworkChannel, KafkaRaftClient, KafkaRaftClientDriver, LeaderAndEpoch, QuorumConfig, RaftClient, ReplicatedLog}
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at

// After
import org.apache.kafka.raft.{Isolation, KafkaRaftClient, LogAppendInfo, LogFetchInfo, LogOffsetMetadata, OffsetAndEpoch, OffsetMetadata, ReplicatedLog, SegmentPosition, ValidOffsetAndEpoch}
        Optional.of(new SegmentPosition(
      Optional.of(new SegmentPosition(
      Optional.of(new SegmentPosition(hwm.segmentBaseOffset, hwm.relativePositionInSegment))
import org.apache.kafka.raft.{Endpoints, FileQuorumStateStore, KafkaNetworkChannel, KafkaRaftClient, KafkaRaftClientDriver, LeaderAndEpoch, QuorumConfig, RaftClient, ReplicatedLog, TimingWheelExpirationService}
```

**How to spot**
When reviewing core/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15599: move segmentposition/timingwheelexpirationservice to raft module (#18094).

**Test**
No test added.
