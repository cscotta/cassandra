**Commit**: 3d9f88daf3 · **Ticket**: KAFKA-17546
**Subsystem**: bin/kafka-groups.sh
**Category**: Logic error in condition

**Root cause**
This implements the kafka-groups.sh tool and Admin.listGroups method defined in KIP-1043. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

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
When reviewing bin/kafka-groups.sh code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17546 admin.listgroups and kafka-groups.sh (#17626).

**Test**
Test modified in AdminClientTestUtils.java covering the corrected behaviour.
