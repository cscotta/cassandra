**Commit**: 74acbd200d · **Ticket**: KAFKA-16758
**Subsystem**: checkstyle/suppressions.xml
**Category**: Resource leak

**Root cause**
JIRA: [KAFKA-16758](https://issues.apache.org/jira/browse/KAFKA-16758)
This PR is aim to deliver

[KIP-1092](https://cwiki.apache.org/confluence/pages/viewpage.action?pageId=321719077),
please refer to KIP-1092 and KAFKA-16758 for further details. Reviewers: Anna Sophie Blee-Goldman <ableegoldman@apache.org>, Chia-Ping
Tsai <chia7712@gmail.com>, Kirk True <kirk@kirktrue.pro>

**Before/After**
```java
// Before
              files="RequestResponseTest.java|FetcherTest.java|FetchRequestManagerTest.java|KafkaAdminClientTest.java"/>

// After
              files="RequestResponseTest.java|FetcherTest.java|FetchRequestManagerTest.java|KafkaAdminClientTest.java|ConsumerMembershipManagerTest.java"/>
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
When reviewing checkstyle/suppressions.xml code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16758: extend consumer#close with an option to leave the group or not (#17614).

**Test**
Test modified in AbstractCoordinatorTest.java covering the corrected behaviour.
