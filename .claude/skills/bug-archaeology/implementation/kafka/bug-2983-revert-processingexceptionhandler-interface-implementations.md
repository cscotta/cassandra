**Commit**: 76d1f18e42 · **Ticket**: KAFKA-16448
**Subsystem**: streams/src
**Category**: Resource leak

**Root cause**
This reverts commit 8d11d9579503426edfaeae791ec4bb212da37ad2. We decided to not release KIP-1033 with AK 3.8

Reviewer: Lucas Brutschy <lbrutschy@confluent.io>

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
When reviewing streams/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: revert "kafka-16448: add processingexceptionhandler interface and implementations (#16090)" (#16142).

**Test**
No test added.
