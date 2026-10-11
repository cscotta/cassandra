**Commit**: 252c1acac3 · **Ticket**: KAFKA-16740
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
The PR adds skeleton code for Share Fetch and Acknowledge RPCs. The changes include:

1. Defining RPCs in KafkaApis.scala
2. Added new SharePartitionManager class which handles the RPCs handling
3.

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
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16740: adding skeleton code for share fetch and acknowledge rpc (kip-932) (#16184).

**Test**
Test modified in SharePartitionTest.java covering the corrected behaviour.
