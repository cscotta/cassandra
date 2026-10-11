**Commit**: d64f4b9cd0 · **Ticket**: KAFKA-16714
**Subsystem**: bin/kafka-share-groups.sh
**Category**: Logic error in condition

**Root cause**
Introduces kafka-share-groups.sh for listing and describing share groups. The PR also contains the remaining options in the command parser and usage message in preparation of their implementation. Reviewers:  Manikumar Reddy <manikumar.reddy@gmail.com>, Apoorv Mittal <apoorvmittal10@gmail.com>

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
When reviewing bin/kafka-share-groups.sh code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16714: kafka-share-groups.sh list and describe (#16835).

**Test**
Test modified in ShareGroupCommandTest.java covering the corrected behaviour.
