**Commit**: 63d65c6899 · **Ticket**: KAFKA-17511
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

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
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

    public Set<TopicPartition> topicPartitions() {
        if (this.data.topicPartitions() == null) {
            return Collections.emptySet();
        }
        return this.data.topicPartitions().stream()
            .flatMap(topicPartition -> topicPartition.partitions().stream()
                    .map(partitionId -> new TopicPartition(topicPartition.topic(), partitionId))
            )
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17511 move electleadersrequestops to electleadersrequest (#17312).

**Test**
No test added.
