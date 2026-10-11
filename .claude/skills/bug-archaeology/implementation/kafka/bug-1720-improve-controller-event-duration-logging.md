**Commit**: c4840f5e93 · **Ticket**: KAFKA-16446
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
There are times when the controller has a high event processing time, such as during startup, or when creating a topic with many partitions. We can see these processing times in the p99 metric (kafka.controller:type=ControllerEventManager,name=EventQueueProcessingTimeMs), however it's difficult to see exactly which event is causing high processing time. With DEBUG logs, we see every event along with its processing time. Even with this, it's a bit tedious to find the event with a high processing time.

**Before/After**
```java
// Before
          setInterBrokerListenerName(config.interBrokerListenerName.value())

// After
          setInterBrokerListenerName(config.interBrokerListenerName.value()).
          setControllerPerformanceSamplePeriodMs(config.controllerPerformanceSamplePeriodMs).
          setControllerPerformanceAlwaysLogThresholdMs(config.controllerPerformanceAlwaysLogThresholdMs)
  val controllerPerformanceSamplePeriodMs: Long = getLong(KRaftConfigs.CONTROLLER_PERFORMANCE_SAMPLE_PERIOD_MS)
  val controllerPerformanceAlwaysLogThresholdMs: Long = getLong(KRaftConfigs.CONTROLLER_PERFORMANCE_ALWAYS_LOG_THRESHOLD_MS)
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16446: improve controller event duration logging (#15622).

**Test**
Test modified in EventPerformanceMonitorTest.java covering the corrected behaviour.
