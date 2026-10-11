**Commit**: 1d0333f83f · **Ticket**: KAFKA-18703
**Subsystem**: trogdor/src
**Category**: Logic error in condition

**Root cause**
the usage of `PayloadKeyType` was removed by
https://github.com/apache/kafka/commit/a70e4f95d713dd34757b3807171a2e520dd9c04d

Reviewers: Jhen-Yung Hsu <jhenyunghsu@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,

// After
// (no additions)
```

**How to spot**
When reviewing trogdor/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18703 remove unused class payloadkeytype (#19129).

**Test**
No test added.
