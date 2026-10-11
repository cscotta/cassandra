**Commit**: 3884062d25 · **Ticket**: KAFKA-20297
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
MappedIterator and related tests are no longer needed

Reviewers: Chia-Ping Tsai <chia7712@gmail.com>, Nilesh Kumar
 <nileshkumar3@gmail.com>, Ken Huang <s7133700@gmail.com>

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
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20297 remove mappediterator and test (#21719).

**Test**
Test modified in MappedIteratorTest.java covering the corrected behaviour.
