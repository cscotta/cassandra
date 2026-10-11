**Commit**: 4a6c6d1a1e · **Ticket**: KAFKA-18284
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
KIP-1071 specifies records that the group coordinator needs to store
into the consumer offset topic to persist the state of a Streams
group. This records are specified in json files from which the actual
classes for the records are generated. This commit adds the records needed by the group coordinator to store
the state of a Streams group. Reviewer: Lucas Brutschy <lbrutschy@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After
// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements.  See the NOTICE file distributed with
// this work for additional information regarding copyright ownership.
// The ASF licenses this file to You under the Apache License, Version 2.0
// (the "License"); you may not use this file except in compliance with
// the License.  You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18284: add group coordinator records for streams rebalance protocol (#18228).

**Test**
No test added.
