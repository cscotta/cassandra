**Commit**: d7fed9d023 · **Ticket**: KAFKA-19709
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Extracts the handling of `.checkpoint` files, which will become a legacy
feature as part of KIP-1035, to a separate
`LegacyCheckpointingStateStore`, which will wrap any `StateStore` that
needs it. This implementation is currently unused, as the changes that introduce
its usage will be made as part of a follow-up PR. A core goal of this implementation is to fully encapsulate
"checkpointing", and the management of `.checkpoint` files within this
class. Consequently, there are a few behavioural differences:

1.

**Before/After**
```java
// Before
    File globalStateDir() {

// After
    public File globalStateDir() {
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
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19709: add legacycheckpointingstatestore (#21554).

**Test**
Test modified in LegacyCheckpointingStateStore.java covering the corrected behaviour.
