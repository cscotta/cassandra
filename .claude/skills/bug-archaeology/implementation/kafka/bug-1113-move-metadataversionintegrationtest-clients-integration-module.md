**Commit**: 19530738c4 · **Ticket**: KAFKA-19240
**Subsystem**: clients/clients-integration-tests
**Category**: Logic error in condition

**Root cause**
The PR do following:
1. Move MetadataVersionIntegrationTest to clients-integration-tests
module
2. rewrite to java from scala

Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
/*
 * Licensed to the Apache Software Foundation (ASF) under

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
When reviewing clients/clients-integration-tests code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19240 move metadataversionintegrationtest to clients-integration-tests module (#19641).

**Test**
Test modified in MetadataVersionIntegrationTest.java covering the corrected behaviour.
