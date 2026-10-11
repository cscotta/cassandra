**Commit**: 8209af2a23 · **Ticket**: KAFKA-20064
**Subsystem**: clients/src
**Category**: Race condition / missing synchronization

**Root cause**
* Introduced a new class - PartitionLeaderCache
* Changed the usage of the cache to make calls atomic(e.g. getting
cached and non-cached values as one call instead of two, deleting cached
values as one call, not one by one)
* Added an integration test that tests the concurrent access of the
cache

Reviewers: Andrew Schofield <aschofield@confluent.io>

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
When reviewing clients/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20064: made partitionleadercache thread safe. (#21335).

**Test**
Test modified in ConcurrentListOffsetsRequestTest.java covering the corrected behaviour.
