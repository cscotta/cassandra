**Commit**: 84bcdc95af · **Ticket**: KAFKA-17540
**Subsystem**: committer-tools/update-cache.sh
**Category**: Logic error in condition

**Root cause**
Uses the `gh` CLI to find the latest trunk commit which has been cached by GitHub actions. By basing PRs off of this
ref rather than HEAD, we will see fewer cache misses in our CI builds. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>, David Arthur <mumrah@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
#!/bin/bash
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
```

**How to spot**
When reviewing committer-tools/update-cache.sh code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17540: create script for updating a reference of latest cached trunk commit (#17204).

**Test**
No test added.
