**Commit**: 6e88c10ed5 · **Ticket**: KAFKA-14483
**Subsystem**: checkstyle/suppressions.xml
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
              files="(LogLoader|LogValidator|RemoteLogManager|RemoteIndexCache).java"/>
/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software

// After
              files="(LocalLog|LogLoader|LogValidator|RemoteLogManager|RemoteIndexCache).java"/>
```

**How to spot**
When reviewing checkstyle/suppressions.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-14483 move locallog to storage module (#17587).

**Test**
Test modified in PartitionLockTest.scala covering the corrected behaviour.
