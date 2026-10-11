**Commit**: 74ebbae8ec · **Ticket**: KAFKA-19132
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Mickael Maison <mickael.maison@gmail.com>

**Before/After**
```java
// Before
import kafka.server.FetchManager;
import org.apache.kafka.server.{AssignmentsManager, BrokerFeatures, BrokerLifecycleManager, ClientMetricsManager, DefaultApiVersionManager, DelayedActionQueue, KRaftTopicCreator, NodeToControllerChannelManagerImpl, ProcessRole, RaftControllerNodeProvider}
      val fetchSessionCacheShards = (0 until NumFetchSessionCacheShards)
        .map(shardNum => new FetchSessionCacheShard(
/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *

// After
import org.apache.kafka.server.FetchManager;
import org.apache.kafka.server.FetchSession.FetchSessionCache
import org.apache.kafka.server.{AssignmentsManager, BrokerFeatures, BrokerLifecycleManager, ClientMetricsManager, DefaultApiVersionManager, DelayedActionQueue, FetchManager, FetchSessionCacheShard, KRaftTopicCreator, NodeToControllerChannelManagerImpl, ProcessRole, RaftControllerNodeProvider}
      val fetchSessionCacheShards: util.List[FetchSessionCacheShard] = new util.ArrayList()
      for (shardNum <- 0 until NumFetchSessionCacheShards) {
        fetchSessionCacheShards.add(new FetchSessionCacheShard(
      }
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19132: move fetchsession and related classes to server module (#20158).

**Test**
Test modified in FetchSessionTest.scala covering the corrected behaviour.
