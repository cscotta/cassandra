**Commit**: 2696a6d7a1 · **Ticket**: KAFKA-17767
**Subsystem**: .github/actions
**Category**: Logic error in condition

**Root cause**
Add a new "load-catalog" job to the workflow. This job will checkout the test-catalog branch at 7 days prior and generate a text file of all the tests that were known at that time. This file is then passed down to the two parallel "test" jobs to be used as a source of data for the quarantined test behavior. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
```

**How to spot**
When reviewing .github/actions code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17767 load test catalog data [3/n] (#17654).

**Test**
Test modified in format-test-catalog.py covering the corrected behaviour.
