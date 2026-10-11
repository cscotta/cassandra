**Commit**: cc0f06554b · **Ticket**: KAFKA-19042
**Subsystem**: clients/clients-integration-tests
**Category**: Logic error in condition

**Root cause**
move GroupAuthorizerIntegrationTest to clients-integration-tests module

Reviewers: Ken Huang <s7133700@gmail.com>, PoAn Yang
 <payang@apache.org>, keemsisi <keemsisi@gmail.com>, Chia-Ping Tsai
 <chia7712@gmail.com>

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
When reviewing clients/clients-integration-tests code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19042 move groupauthorizerintegrationtest to clients-integration-tests module (#19685).

**Test**
Test modified in GroupAuthorizerIntegrationTest.java covering the corrected behaviour.
