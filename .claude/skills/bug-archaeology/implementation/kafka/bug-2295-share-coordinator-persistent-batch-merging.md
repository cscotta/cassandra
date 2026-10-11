**Commit**: 7b7eb6243f · **Ticket**: KAFKA-17367
**Subsystem**: checkstyle/import-control-share-coordinator.xml
**Category**: Logic error in condition

**Root cause**
This patch introduces a merging algorithm for persistent state batches in the share coordinator. The algorithm removes any expired batches (lastOffset before startOffset) and then places the rest in a sorted map. It then identifies batch pairs which overlap and combine them while preserving the relative priorities of any intersecting sub-ranges. The resultant batches are placed back into the map.

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
            <allow pkg="org.junit.jupiter.params" />
            <allow pkg="org.junit.jupiter.provider" />
    <!-- share coordinator -->
    <suppress checks="NPathComplexity"
              files="ShareCoordinatorShard.java"/>
    <suppress checks="CyclomaticComplexity"
              files="ShareCoordinatorShard.java"/>

```

**How to spot**
When reviewing checkstyle/import-control-share-coordinator.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17367: share coordinator persistent batch merging algorithm. [3/n] (#17149).

**Test**
Test modified in PersisterStateBatchCombinerTest.java covering the corrected behaviour.
