**Commit**: 30b1bdfc74 · **Ticket**: KAFKA-17835
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before

            <allow pkg="org.apache.kafka.common.config" />
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

// After
    implementation project(':server-common')
    implementation libs.slf4jApi

    testImplementation project(':clients').sourceSets.test.output
    testImplementation project(':test-common')
    testImplementation project(':test-common:test-common-api')
            <allow pkg="org.apache.kafka.clients" />
            <allow pkg="org.apache.kafka.server.common" />
            <allow pkg="org.apache.kafka.clients" />
            <allow pkg="org.apache.kafka.coordinator.transaction" />
            <allow pkg="org.apache.kafka.common" />
            <allow pkg="org.slf4j" />
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17835 move produceridmanager and rpcproduceridmanager to transaction-coordinator module (#17562).

**Test**
Test modified in NodeToControllerRequestThreadTest.scala covering the corrected behaviour.
