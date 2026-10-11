**Commit**: aea699bdef · **Ticket**: KAFKA-18324
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
Implements the current assignment builder, analogous to the current assignment builder of consumer groups. The main difference is the underlying assigned resource, and slightly different logic around process IDs: We make sure to move a task only to a new client, once the task is not owned anymore by any client with the same process ID (sharing the same state directory) - in any role (active, standby or warm-up). Compared to the feature branch, the main difference is that I refactored the separate treatment of active, standby and warm-up tasks into a compound datatype called TaskTuple (which is used in place of the more specific Assignment class). This also has effects on StreamsGroupMember.

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
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18324: add currentassignmentbuilder (#18476).

**Test**
Test modified in CurrentAssignmentBuilderTest.java covering the corrected behaviour.
