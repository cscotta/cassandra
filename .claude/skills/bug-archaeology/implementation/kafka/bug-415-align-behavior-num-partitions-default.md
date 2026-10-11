**Commit**: 288b4799ee · **Ticket**: KAFKA-19655
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Currently, 'num.partitions' and 'default.replication.factor' are applied
inconsistently. Topic auto-creation relies on Broker configs, while
Streams and AdminClient rely on Controller configs. This leads to
confusing behavior where a Broker and Controller might   have diverging
defaults. This commit implements the 4.x transition phase:
- Updated DefaultAutoTopicCreationManager to check if these configs are
  explicitly set in 'broker.properties'.

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
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19655: align the behavior of num.partitions and default.replication.factor for topic creation (#21550).

**Test**
Test modified in AutoTopicCreationTest.java covering the corrected behaviour.
