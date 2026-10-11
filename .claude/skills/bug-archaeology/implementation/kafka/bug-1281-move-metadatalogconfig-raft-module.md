**Commit**: 121ec2a662 · **Ticket**: KAFKA-15599
**Subsystem**: checkstyle/import-control.xml
**Category**: Wrong serialization / deserialization

**Root cause**
Rewrite the class in Java and move it to the raft module. Reviewers: PoAn Yang <payang@apache.org>, TengYao Chi
<kitingiao@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,

// After
    <allow pkg="org.apache.kafka.server.config" />
import org.apache.kafka.raft.{Isolation, KafkaRaftClient, LogAppendInfo, LogFetchInfo, LogOffsetMetadata, MetadataLogConfig, OffsetAndEpoch, OffsetMetadata, ReplicatedLog, SegmentPosition, ValidOffsetAndEpoch}
        config.deleteDelayMillis
import org.apache.kafka.raft.{Endpoints, ExternalKRaftMetrics, FileQuorumStateStore, KafkaNetworkChannel, KafkaRaftClient, KafkaRaftClientDriver, LeaderAndEpoch, MetadataLogConfig, QuorumConfig, RaftClient, ReplicatedLog, TimingWheelExpirationService}
import org.apache.kafka.server.config.ServerLogConfigs
      config = n
```

**How to spot**
When reviewing checkstyle/import-control.xml code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15599 move metadatalogconfig to raft module (#19246).

**Test**
Test modified in KafkaMetadataLogTest.scala covering the corrected behaviour.
