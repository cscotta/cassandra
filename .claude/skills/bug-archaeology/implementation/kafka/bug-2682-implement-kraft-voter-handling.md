**Commit**: 9db5c2481f · **Ticket**: KAFKA-16535
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
This change implements the AddVoter RPC. The high-level algorithm is as follow:

1. Check that the leader has fenced the previous leader(s) by checking that the HWM is known;
   otherwise, return the REQUEST_TIMED_OUT error. 2.

**Before/After**
```java
// Before
    INVALID_VOTER_KEY(125, "The voter key doesn't match the receiving replica's key.", InvalidVoterKeyException::new);

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
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16535; implement kraft add voter handling.

**Test**
Test modified in KafkaRaftClientReconfigTest.java covering the corrected behaviour.
