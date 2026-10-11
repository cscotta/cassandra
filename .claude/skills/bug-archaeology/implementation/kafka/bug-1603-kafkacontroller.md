**Commit**: 0e502e0b47 · **Ticket**: KAFKA-18431
**Subsystem**: core/src
**Category**: State not cleaned up

**Root cause**
Remove KafkaController and related unused references:

* ControllerChannelContext
* ControllerChannelManager
* ControllerEventManager
* ControllerState
* PartitionStateMachine
* ReplicaStateMachine
* TopicDeletionManager
* ZkBrokerEpochManager

Reviewers: Ismael Juma <ismael@juma.me.uk>

**Before/After**
```java
// Before
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

// After
// (no additions)
```

**How to spot**
When reviewing core/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18431: remove kafkacontroller (#18573).

**Test**
Test modified in QuorumTestHarness.scala covering the corrected behaviour.
