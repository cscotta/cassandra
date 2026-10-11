**Commit**: a9bce0647f · **Ticket**: KAFKA-19535
**Subsystem**: clients/clients-integration-tests
**Category**: Logic error in condition

**Root cause**
Add tests for producer state listing with, without, and invalid
brokerId. Reviewers: TengYao Chi <kitingiao@gmail.com>, Chia-Ping Tsai
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
When reviewing clients/clients-integration-tests code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19535 add integration tests for describeproducersoptions#brokerid (#20420).

**Test**
Test modified in DescribeProducersWithBrokerIdTest.java covering the corrected behaviour.
