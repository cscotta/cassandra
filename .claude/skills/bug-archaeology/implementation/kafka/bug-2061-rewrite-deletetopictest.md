**Commit**: 393455eb1a · **Ticket**: KAFKA-17837
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
    implementation project(':storage')
    <allow pkg="kafka.log"/>
    <allow pkg="org.apache.kafka.metadata"/>
    <allow pkg="org.apache.kafka.storage.internals.checkpoint"/>
    <allow pkg="org.apache.kafka.storage.internals.log"/>
    <allow pkg="scala.jdk.javaapi" />
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17837 rewrite deletetopictest (#17579).

**Test**
Test modified in import-control-test-common.xml covering the corrected behaviour.
