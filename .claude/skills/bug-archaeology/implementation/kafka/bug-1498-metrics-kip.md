**Commit**: 184b891871 · **Ticket**: KAFKA-16524
**Subsystem**: core/src
**Category**: Race condition / missing synchronization

**Root cause**
This change implement some of the metrics enumerated in KIP-853. The KafkaRaftMetrics object now exposes number-of-voters, number-of-observers and uncommitted-voter-change. The number-of-observers and uncommitted-voter-change metrics are only present on the active controller or leader, since it does not make sense for other replicas to report these metrics. In order to make these two metrics thread-safe, KafkaRaftMetrics needs to be passed into LeaderState, and therefore QuorumState.

**Before/After**
```java
// Before
import org.apache.kafka.raft.{Endpoints, FileQuorumStateStore, KafkaNetworkChannel, KafkaRaftClient, KafkaRaftClientDriver, LeaderAndEpoch, QuorumConfig, RaftClient, ReplicatedLog, TimingWheelExpirationService}
      metrics
import kafka.raft.KafkaRaftManager

// After
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
```

**How to spot**
When reviewing core/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16524; metrics for kip-853 (#18304).

**Test**
Test modified in TestRaftServer.scala covering the corrected behaviour.
