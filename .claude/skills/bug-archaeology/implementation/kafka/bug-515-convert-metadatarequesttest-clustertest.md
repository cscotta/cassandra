**Commit**: 0781de247f · **Ticket**: KAFKA-12617
**Subsystem**: checkstyle/suppressions.xml
**Category**: Resource leak

**Root cause**
# Description
Changed `MetadataRequestTest` to use `ClusterTest`

# Changes
Added `ClusterInstance#createTopicWithAssignment` to align with
`KafkaServerTestHarness#createTopicWithAssignment`

Helper functions inherited from base classes were rewritten in
`AbstractMetadataRequestTest`

# Issue
`testIsrAfterBrokerShutDownAndJoinsBack` fails because of thread leak:
```
org.opentest4j.AssertionFailedError: Thread leak detected: JMX server
connection timeout 28 ==>
Expected :true
Actual   :false
```

`testAliveBrokersWithNoTopics` would also fail if the last
`checkMetadata` is removed. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,

// After
    <suppress checks="ImportControl" files="MetadataRequestTest.java"/>
```

**How to spot**
When reviewing checkstyle/suppressions.xml code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-12617 convert metadatarequesttest to use clustertest (#21101).

**Test**
Test modified in AbstractMetadataRequestTest.scala covering the corrected behaviour.
