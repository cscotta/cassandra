**Commit**: ad031b99d3 · **Ticket**: KAFKA-18635
**Subsystem**: metadata/src
**Category**: Logic error in condition

**Root cause**
We need to re-enable the unclean shutdown detection when in ELR mode, which was inadvertently removed during the development process. Reviewers: David Mao <dmao@confluent.io>,  Jun Rao <junrao@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
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
 * distributed under the License is distributed on an "AS IS" BASIS,
```

**How to spot**
When reviewing metadata/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18635: reenable the unclean shutdown detection (#18277).

**Test**
Test modified in EligibleLeaderReplicasIntegrationTest.java covering the corrected behaviour.
