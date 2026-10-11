**Commit**: bf0e6ba700 · **Ticket**: KAFKA-19384
**Subsystem**: checkstyle/suppressions.xml
**Category**: Resource leak

**Root cause**
Fixes a false positive in `BrokerRegistrationRequestTest` caused by
`isMigratingZkBroker`, and migrates the test from Scala to Java. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

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
    <suppress checks="ImportControl" files="BrokerRegistrationRequestTest.java"/>
```

**How to spot**
When reviewing checkstyle/suppressions.xml code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19384 the passing of brokerregistrationrequesttest is a false positive (#20338).

**Test**
Test modified in BrokerRegistrationRequestTest.scala covering the corrected behaviour.
