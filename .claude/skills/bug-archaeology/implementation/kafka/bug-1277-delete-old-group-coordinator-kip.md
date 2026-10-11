**Commit**: ca20e9cd92 · **Ticket**: KAFKA-18329
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
This patch is the third of a series of patches to remove the old group
coordinator. With the release of Apache Kafka 4.0, the so-called new
group coordinator is the default and only option available now. It removes the old group coordinator and cleans up the
`GroupCoordinator` interface. Reviewers: Jeff Kim <jeff.kim@confluent.io>, Chia-Ping Tsai <chia7712@gmail.com>

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
// (no additions)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18329; [3/3] delete old group coordinator (kip-848) (#19255).

**Test**
Test modified in GroupCoordinatorAdapterTest.scala covering the corrected behaviour.
