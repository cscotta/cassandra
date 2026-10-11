**Commit**: 4e77466f6a · **Ticket**: KAFKA-19170
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
rewrite `MetricsDuringTopicCreationDeletionTest` to `ClusterTest` infra
and move it to clients-integration-tests module. Reviewers: PoAn Yang <payang@apache.org>, Ken Huang
<s7133700@gmail.com>, Jhen-Yung Hsu <jhenyunghsu@gmail.com>, Chia-Ping
Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
    testImplementation libs.metrics
    <allow pkg="com.yammer.metrics.core" />

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19170 move metricsduringtopiccreationdeletiontest to client-integration-tests module (#19528).

**Test**
Test modified in import-control-clients-integration-tests.xml covering the corrected behaviour.
