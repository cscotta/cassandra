**Commit**: ee3f80eda8 · **Ticket**: KAFKA-18887
**Subsystem**: tools/src
**Category**: Logic error in condition

**Root cause**
This PR adds integration tests for `--list`

(Transferred from the feature branch `kip1071`)  related ticket:
[KAFKA-18887](https://issues.apache.org/jira/browse/KAFKA-18887)

Reviewers: Lucas Brutschy <lbrutschy@confluent.io>

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
When reviewing tools/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: minor: add test for kafka-streams-groups.sh list (#19894).

**Test**
Test modified in ListStreamsGroupTest.java covering the corrected behaviour.
