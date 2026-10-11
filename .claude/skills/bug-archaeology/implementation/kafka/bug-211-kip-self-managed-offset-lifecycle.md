**Commit**: 4d0a85f248 · **Ticket**: KAFKA-20417
**Subsystem**: streams/integration-tests
**Category**: Resource leak

**Root cause**
Adds `SelfManagedOffsetLifecycleIntegrationTest` — companion to the
recovery tests. Validates the happy path for column-family-based offset management. After each clean shutdown, tests directly inspect the CF to assert
status=closed and offsets populated. Covers:
- State preservation across clean restarts                    - Multiple
restart cycles    - No changelog restoration after clean shutdown (via
StateRestoreListener)    - Clean restart with no data processed
- Interactive query state verification across restarts

All tests parameterized for ALOS and EOS.

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
When reviewing streams/integration-tests code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20417: add kip-1035 self-managed offset lifecycle integration tests (#21999).

**Test**
Test modified in SelfManagedOffsetLifecycleIntegrationTest.java covering the corrected behaviour.
