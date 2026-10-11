**Commit**: 24dd11d693 · **Ticket**: KAFKA-17593
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
This patch introduces the asynchronous resolution of regular expressions. Let me unpack a few details about the implementations:
1) I have decided to finally update all the regular expressions within a consumer group together. My assumption is that the number of regular expressions in a group will be generally small but the number of topics in a cluster is large. Hence grouping has two benefits.

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
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17593; [8/n] resolve regular expressions (#17864).

**Test**
Test modified in MockCoordinatorExecutor.java covering the corrected behaviour.
