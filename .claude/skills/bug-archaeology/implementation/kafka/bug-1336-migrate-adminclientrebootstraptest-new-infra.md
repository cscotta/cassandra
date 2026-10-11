**Commit**: 870db5d811 · **Ticket**: KAFKA-18915
**Subsystem**: checkstyle/import-control-core.xml
**Category**: Wrong serialization / deserialization

**Root cause**
Migrate AdminClientRebootstrapTest to the new test infra and remove the
old Scala test. Reviewers: TengYao Chi <kitingiao@gmail.com>, David Arthur <mumrah@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
  <allow pkg="org.apache.kafka.clients" class="CommonClientConfigs"/>
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
 * Unless required by applicable law or agreed to in writing, software
```

**How to spot**
When reviewing checkstyle/import-control-core.xml code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18915: migrate adminclientrebootstraptest to use new test infra (#19094).

**Test**
Test modified in AdminClientRebootstrapTest.java covering the corrected behaviour.
