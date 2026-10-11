**Commit**: 00b1b1a144 · **Ticket**: KAFKA-17628
**Subsystem**: .github/actions
**Category**: Logic error in condition

**Root cause**
This patch allows workflow runs to be controlled by the ci-approved label on Pull Requests. Rather than manually approving each workflow run explicitly, committers can now add the appropriate label and the new "CI Requested" and "PR Labeled" workflows will auto-approve the requested run. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

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
When reviewing .github/actions code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17628 new workflows for automating run approvals (#17290).

**Test**
No test added.
