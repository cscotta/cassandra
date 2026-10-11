**Commit**: 3636afb737 · **Ticket**: KAFKA-17048
**Subsystem**: config/kraft
**Category**: Logic error in condition

**Root cause**
HOTFIX: add file that was missed from KAFKA-17048 commit. The commit addresses a correctness issue in the config/kraft subsystem. The fix ensures the code behaves correctly under all expected conditions.

**Before/After**
```java
// Before
// (no removals)

// After
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
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
```

**How to spot**
When reviewing config/kraft code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: hotfix: add file that was missed from kafka-17048 commit.

**Test**
No test added.
