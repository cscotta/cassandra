**Commit**: 75bcb9eb42 · **Ticket**: KAFKA-17239
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
Reviewers: TengYao Chi <kitingiao@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
import org.apache.kafka.clients.admin.internals.AdminFetchMetricsManager;
    private final AdminFetchMetricsManager adminFetchMetricsManager;
        this.adminFetchMetricsManager = new AdminFetchMetricsManager(metrics);
                        adminFetchMetricsManager.recordLatency(response.destination(), response.requestLatencyMs());
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17239 add request-latency metrics for node in admin client (#16832).

**Test**
Test modified in AdminFetchMetricsManagerTest.java covering the corrected behaviour.
