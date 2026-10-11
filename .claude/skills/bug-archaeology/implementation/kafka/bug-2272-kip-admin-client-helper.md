**Commit**: c11a38f9df · **Ticket**: KAFKA-17248
**Subsystem**: checkstyle/suppressions.xml
**Category**: Logic error in condition

**Root cause**
No functional changes, this PR contains a test-helper class for working with AdminClient

Reviewers Matthias Sax <mjsax@apache.org>

**Before/After**
```java
// Before
              files="(AbstractFetch|Sender|SenderTest|ConsumerCoordinator|KafkaConsumer|KafkaProducer|Utils|TransactionManager|TransactionManagerTest|KafkaAdminClient|NetworkClient|Admin|KafkaRaftClient|KafkaRaftClientTest|RaftClientTestContext).java"/>

// After
              files="(AbstractFetch|Sender|SenderTest|ConsumerCoordinator|KafkaConsumer|KafkaProducer|Utils|TransactionManager|TransactionManagerTest|KafkaAdminClient|NetworkClient|Admin|KafkaRaftClient|KafkaRaftClientTest|RaftClientTestContext|TestingMetricsInterceptingAdminClient).java"/>
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
When reviewing checkstyle/suppressions.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17248: kip-1076 add admin client test helper [1/n] (#17375).

**Test**
Test modified in TestingMetricsInterceptingAdminClient.java covering the corrected behaviour.
