**Commit**: 35489bfca3 · **Ticket**: KAFKA-18311
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
Clients in the Streams Rebalance Protocol send an "unconfigured" representation of the topology to the broker. That is, the number of input topics and (some) internal topics is not fixed, regular expressions are not resolved. The broker takes this description of the topology and, together with the current state of the topics on the broker, derives a ConfiguredTopology. The configured topology is what is being returned from StreamsGroupDescribe, and has all number of partitions defined, and regular expressions resolved.

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
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18311: add internal datastructure for configuring topologies (1/n) (#18268).

**Test**
Test modified in ConfiguredInternalTopicTest.java covering the corrected behaviour.
