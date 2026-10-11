**Commit**: 5bb1ea403c · **Ticket**: KAFKA-18223
**Subsystem**: .github/scripts
**Category**: Logic error in condition

**Root cause**
Adds a python script to generate a detailed flaky test report using the Develocity API

Reviewers: David Arthur <mumrah@gmail.com>

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
When reviewing .github/scripts code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18223 flaky test report script (#17938).

**Test**
No test added.
