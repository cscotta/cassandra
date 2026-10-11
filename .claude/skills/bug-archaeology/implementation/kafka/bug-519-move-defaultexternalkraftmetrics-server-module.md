**Commit**: 6be062a690 · **Ticket**: KAFKA-15599
**Subsystem**: checkstyle/import-control-server.xml
**Category**: Logic error in condition

**Root cause**
Another small step in moving all of `kafka.raft` classes out of the
`core` module. The `DefaultExternalKRaftMetrics` class needs:
- `ControllerMetadataMetrics` from `metadata`
- `ExternalKRaftMetrics` from `raft`
-  `BrokerServerMetrics` from `server`. The `server` module already depends on `metadata` and `raft`. It also
already has a bunch of metrics related classes in
`org.apache.kafka.server.metrics`, so it seems the best fit.

**Before/After**
```java
// Before
import kafka.raft.{DefaultExternalKRaftMetrics, KafkaRaftManager}
import org.apache.kafka.server.metrics.{BrokerServerMetrics, KafkaYammerMetrics, NodeMetrics}
        val externalKRaftMetrics = new DefaultExternalKRaftMetrics(Option(brokerMetrics), Option(controllerServerMetrics))
import kafka.raft.{DefaultExternalKRaftMetrics, KafkaRaftManager}
      new DefaultExternalKRaftMetrics(None, None),
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at

// After
      <allow class="org.apache.kafka.controller.metrics.ControllerMetadataMetrics" />
import kafka.raft.KafkaRaftManager
import org.apache.kafka.server.metrics.{BrokerServerMetrics, DefaultExternalKRaftMetrics, KafkaYammerMetrics, NodeMetrics}
        val externalKRaftMetrics = new DefaultExternalKRaftMetrics(Optional.ofNullable(brokerMetrics), Optional.ofNullable(controllerServerMetrics))
import kafka.raft.KafkaRaftManager
import org.apache.kafka.server.metrics.DefaultExternalKRaftMetrics
import java.util.Optional
      new DefaultExternalKRaftMetrics(Optional.empty, Optional.empty),
```

**How to spot**
When reviewing checkstyle/import-control-server.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15599: move defaultexternalkraftmetrics to the server module (#21163).

**Test**
Test modified in TestRaftServer.scala covering the corrected behaviour.
