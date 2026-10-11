**Commit**: 1697a7b8fe · **Ticket**: KAFKA-20316
**Subsystem**: clients/clients-integration-tests
**Category**: Logic error in condition

**Root cause**
Rewrite `AddPartitionsTest` with `ClusterInstance`. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
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
```

**How to spot**
When reviewing clients/clients-integration-tests code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20316 rewrite addpartitionstest with clusterinstance (#21903).

**Test**
Test modified in AddPartitionsTest.java covering the corrected behaviour.
