**Commit**: a6a0ea56d8 · **Ticket**: KAFKA-17171
**Subsystem**: clients/clients-integration-tests
**Category**: Logic error in condition

**Root cause**
Given that the `core` module will be separated into other small modules,
this test will not be added to the core module. Instead, I added it to the `clients-integration-tests` module since it
focuses on the admin client test. The patch should include following test cases. 1.

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
When reviewing clients/clients-integration-tests code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17171 add test cases for `static_broker_config`in kraft mode (#18463).

**Test**
Test modified in StaticBrokerConfigTest.java covering the corrected behaviour.
