**Commit**: ab0df20489 · **Ticket**: KAFKA-17592
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
This patch includes:
- Bump ConsumerGroupHeartbeatRequest version to include subscribedTopicRegex field
- Introduce new error code for InvalidRegularExpression 
- Bump ConsumerGroupHeartbeatResponse version to support new regex error
- Wire the new field into the GroupMetadataManager when processing HB

Reviewers: David Jacot <djacot@confluent.io>

**Before/After**
```java
// Before
    VOTER_NOT_FOUND(127, "The voter is not part of the set of voters.", VoterNotFoundException::new);
  "validVersions": "0",

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
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17592; support for subscribedtopicsregex in consumergroupheartbeat rpc (#17257).

**Test**
Test modified in GroupMetadataManagerTest.java covering the corrected behaviour.
