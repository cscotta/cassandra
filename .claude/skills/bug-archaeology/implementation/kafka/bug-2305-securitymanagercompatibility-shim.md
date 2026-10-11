**Commit**: 818ee8a581 · **Ticket**: KAFKA-17078
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Signed-off-by: Greg Harris <greg.harris@aiven.io>
Reviewers: José Armando García Sancio <jsancio@apache.org>, Omnia Ibrahim <o.g.h.ibrahim@gmail.com>, Chris Egerton <fearthecellos@gmail.com>, Luke Chen <showuon@gmail.com>, David Arthur <mumrah@gmail.com>

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
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17078: add securitymanagercompatibility shim (#16522).

**Test**
Test modified in CompositeStrategy.java covering the corrected behaviour.
