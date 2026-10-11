**Commit**: 888a177603 · **Ticket**: KAFKA-12708
**Subsystem**: checkstyle/suppressions.xml
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
              files="(Microbenchmarks|SaslServerAuthenticator).java"/>
 * A simple read-optimized map implementation that synchronizes only writes and does a full copy on each modification
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

// After
              files="(SaslServerAuthenticator).java"/>
 * A simple read-optimized map implementation that synchronizes only writes and does a full copy on each modification.
 * <p>
 * Through the {@link org.apache.kafka.jmh.util.ConcurrentMapBenchmark}, we observed that in scenarios where
 * write operations (i.e. computeIfAbsent) constitute 10%, the get performance of CopyOnWriteMap is lower compared to
 * ConcurrentHashMap. However, when iterating over entrySet and values, CopyOnWriteMap performs better than ConcurrentHashMap.
```

**How to spot**
When reviewing checkstyle/suppressions.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-12708 rewrite org.apache.kafka.test.microbenchmarks by jmh (#16231).

**Test**
Test modified in Microbenchmarks.java covering the corrected behaviour.
