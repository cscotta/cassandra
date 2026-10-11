**Commit**: 5b9cbcf886 · **Ticket**: KAFKA-16689
**Subsystem**: checkstyle/import-control-storage.xml
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
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
 * distributed under the License is distributed on an "AS IS" BASIS,

// After
        <allow pkg="kafka.server"/>
        <allow pkg="kafka.log"/>
```

**How to spot**
When reviewing checkstyle/import-control-storage.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16689 move logvalidatortest to storage module (#16167).

**Test**
Test modified in LogValidatorTest.scala covering the corrected behaviour.
