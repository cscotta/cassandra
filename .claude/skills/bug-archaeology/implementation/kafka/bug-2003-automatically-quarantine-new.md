**Commit**: 5f4cbd4aa4 · **Ticket**: KAFKA-17767
**Subsystem**: .github/actions
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
                if not test_case_failed:
                # Collect all tests that were run as part of quarantinedTest
    report_md = f"Download [HTML report]({report_url})."
    summary = (f"{total_run} tests cases run in {duration}. "

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
When reviewing .github/actions code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17767 automatically quarantine new tests [5/n] (#17725).

**Test**
Test modified in UuidTest.java covering the corrected behaviour.
