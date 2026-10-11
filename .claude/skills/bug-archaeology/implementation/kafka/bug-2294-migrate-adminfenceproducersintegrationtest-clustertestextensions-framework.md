**Commit**: 540fb91103 · **Ticket**: KAFKA-17258
**Subsystem**: checkstyle/suppressions.xml
**Category**: Logic error in condition

**Root cause**
Reviewers: Ken Huang <s7133700@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
    <suppress checks="ImportControl" files="AdminFenceProducersTest.java"/>
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
```

**How to spot**
When reviewing checkstyle/suppressions.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17258 migrate adminfenceproducersintegrationtest to clustertestextensions framework (#17311).

**Test**
Test modified in AdminFenceProducersTest.java covering the corrected behaviour.
