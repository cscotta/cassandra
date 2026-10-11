**Commit**: d77f44414d · **Ticket**: KAFKA-18780
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
- Added a unit test to validate the exception hierarchy for all KIP-1050
transaction related exceptions. - RetriableException is correctly extended by all child classes
- Included test for RetriableException exception with verification that
all exceptions extending `RetriableException` do not inadvertently
extend `RefreshRetriableException, preserving the intended behavior. Reviewers: Kirk True <ktrue@confluent.io>, TaiJuWu <tjwu1217@gmail.com>, TengYao Chi <kitingiao@gmail.com>,  Ken Huang <s7133700@gmail.com>, Justine Olshan <jolshan@confluent.io>

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
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18780: extend retriableexception related exceptions (#19020).

**Test**
Test modified in TransactionExceptionHierarchyTest.java covering the corrected behaviour.
