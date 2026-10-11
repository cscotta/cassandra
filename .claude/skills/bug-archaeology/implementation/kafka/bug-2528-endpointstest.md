**Commit**: fb7e47f6e2 · **Ticket**: KAFKA-17169
**Subsystem**: raft/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Omnia Ibrahim <o.g.h.ibrahim@gmail.com>, Colin P. McCabe <cmccabe@apache.org>

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
When reviewing raft/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17169: add endpointstest (#16659).

**Test**
Test modified in EndpointsTest.java covering the corrected behaviour.
