**Commit**: 4ca24a7dbf · **Ticket**: KAFKA-18325
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
A class to build a new target assignment based on the provided parameters. As a result, it yields the records that must be persisted to the log and the new member assignments as a map. Compared to the feature branch, I extended the unit tests (testing also standby and warm-up task logic) and adopted simplifications due to the TasksTuple class. Reviewers: Bruno Cadonna <cadonna@apache.org>, Bill Bejeck <bbejeck@apache.org>

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
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18325: add targetassignmentbuilder (#18676).

**Test**
Test modified in TargetAssignmentBuilderTest.java covering the corrected behaviour.
