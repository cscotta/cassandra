**Commit**: 9a7eee6072 · **Ticket**: KAFKA-16934
**Subsystem**: release.py
**Category**: State not cleaned up

**Root cause**
The current release script has a couple of issues:

* It's a single long file with duplicated logic, which makes
  it difficult to understand and make changes
* When a command fails, the user is forced to start from the
  beginning, expanding feedback loops. e.g. publishing step
  fails because the credentials were set incorrectly in ~/.gradle/gradle.properties

Reviewers: Mickael Maison <mickael.maison@gmail.com>

**Before/After**
```java
// Before
#!/usr/bin/env python

#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#

// After
// (no additions)
```

**How to spot**
When reviewing release.py code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16934: clean up and refactor release.py (#16287).

**Test**
No test added.
