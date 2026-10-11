**Commit**: 917caaee16 · **Ticket**: KAFKA-20417
**Subsystem**: streams/integration-tests
**Category**: Logic error in condition

**Root cause**
Adds `SelfManagedOffsetRecoveryIntegrationTest` and
`RocksDBStoreTestingUtils`

Tests verify Kafka Streams recovers correctly after unclean shutdowns
when offsets are stored in RocksDB column families (KIP-1035). Covers:
- Unclean shutdown recovery (ALOS and EOS)                   - Missing
offsets triggering task corruption detection
- Combined status=open + missing offsets
- EOS exactly-once guarantees preserved after recovery
- Multi-store partial corruption
- Standby task recovery
- KAFKA-19712 regression (standby TaskCorruptedException after state
wipe)

Reviewers: Matthias J. Sax <matthias@confluent.io>

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
When reviewing streams/integration-tests code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20417: add kip-1035 self-managed offset recovery integration tests (#21998).

**Test**
Test modified in SelfManagedOffsetRecoveryIntegrationTest.java covering the corrected behaviour.
