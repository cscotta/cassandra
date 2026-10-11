**Commit**: f7b898167e · **Ticket**: KAFKA-15852
**Subsystem**: checkstyle/import-control-server.xml
**Category**: Logic error in condition

**Root cause**
Convert the test from `QuorumTestHarness` to `@ClusterTest`. Since we can have `@ParameterizedTest` with `@ClusterTest` I created
Classic and Consumer protocol versions of all the tests that use a
consumer. Reviewers: khilesh Chaganti <akhileshchg@users.noreply.github.com>,
 Chia-Ping Tsai <chia7712@gmail.com>

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
    <allow class="kafka.server.KafkaBroker" />
    <allow class="org.apache.kafka.controller.ReplicationControlManager" />
```

**How to spot**
When reviewing checkstyle/import-control-server.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15852 move uncleanleaderelectiontest to server module (#21689).

**Test**
Test modified in UncleanLeaderElectionTest.scala covering the corrected behaviour.
