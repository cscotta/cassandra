**Commit**: 424e7251d6 · **Ticket**: KAFKA-19207
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
1. Move `ForwardingManagerMetrics` and `ForwardingManagerMetricsTest` to
server module. 2. Rewrite them in Java.

**Before/After**
```java
// Before
  val forwardingManagerMetrics: ForwardingManagerMetrics = ForwardingManagerMetrics(metrics, channelManager.getTimeoutMs)
        forwardingManagerMetrics.queueLength.getAndDecrement()
        forwardingManagerMetrics.queueLength.getAndDecrement()
    forwardingManagerMetrics.queueLength.getAndIncrement()
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *

// After
import org.apache.kafka.server.metrics.ForwardingManagerMetrics
  val forwardingManagerMetrics: ForwardingManagerMetrics = new ForwardingManagerMetrics(metrics, channelManager.getTimeoutMs)
        forwardingManagerMetrics.decrementQueueLength()
        forwardingManagerMetrics.decrementQueueLength()
    forwardingManagerMetrics.incrementQueueLength()
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19207 move forwardingmanagermetrics and forwardingmanagermetricstest to server module (#19574).

**Test**
Test modified in ForwardingManagerMetricsTest.scala covering the corrected behaviour.
