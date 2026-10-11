**Commit**: fa62bce63d · **Ticket**: KAFKA-18287
**Subsystem**: bin/kafka-streams-groups.sh
**Category**: Logic error in condition

**Root cause**
Implement the core of kafka-streams-groups.sh for `KIP-1071`
- Implement `--list` and its options: (only `--state`)

Reviewers: Bruno Cadonna <cadonna@apache.org>

**Before/After**
```java
// Before
// (no removals)

// After
#!/bin/bash
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
```

**How to spot**
When reviewing bin/kafka-streams-groups.sh code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18287: add support for kafka-streams-groups.sh --list (#19422).

**Test**
Test modified in StreamsGroupCommandTest.java covering the corrected behaviour.
