**Commit**: 4d23fe92f1 · **Ticket**: KAFKA-16928
**Subsystem**: raft/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Colin P. McCabe <cmccabe@apache.org>

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
When reviewing raft/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16928: test all of the request and response methods in raftutil  (#16517).

**Test**
Test modified in RaftUtilTest.java covering the corrected behaviour.
