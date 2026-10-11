**Commit**: 7d39ba1ef8 · **Ticket**: KAFKA-18311
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
The internal topic manager takes a requested topology and returns a configured topology, as well as any internal topics that need to be created. Shares with the client-side "InternalTopicManager" the name only. Reviewers: Bruno Cadonna <cadonna@apache.org>

**Before/After**
```java
// Before
            throw TopicConfigurationException.missingSourceTopics("No partition count for source topic " + topic);

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
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18311: internal topic manager (5/5) (#18442).

**Test**
Test modified in TopicMetadataTest.java covering the corrected behaviour.
