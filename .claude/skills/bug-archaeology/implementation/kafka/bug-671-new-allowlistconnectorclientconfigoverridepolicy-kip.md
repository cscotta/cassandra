**Commit**: a395d06030 · **Ticket**: KAFKA-19824
**Subsystem**: connect/runtime
**Category**: Logic error in condition

**Root cause**
Reviewers: Chris Egerton <fearthecellos@gmail.com>, Andrew Schofield <aschofield@apache.org>

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
When reviewing connect/runtime code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19824: new allowlistconnectorclientconfigoverridepolicy (kip-1188) (#20750).

**Test**
Test modified in AllowlistConnectorClientConfigOverridePolicyTest.java covering the corrected behaviour.
