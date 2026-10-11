**Commit**: 5cc585669f · **Ticket**: KAFKA-19545
**Subsystem**: server-common/src
**Category**: Logic error in condition

**Root cause**
The `MetadataVersionValidator` class became unused after commit
b6e6a3c68a (KAFKA-18360: Remove zookeeper configurations) removed the
`inter.broker.protocol.version` configuration, which was the only
component using this validator. This commit removes both the validator class and its associated test as
they serve no purpose in the current implementation. Reviewers: Ken Huang <s7133700@gmail.com>, Chia-Ping Tsai
 <chia7712@gmail.com>

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
When reviewing server-common/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19545 remove metadataversionvalidator (#20233).

**Test**
Test modified in MetadataVersionValidatorTest.java covering the corrected behaviour.
