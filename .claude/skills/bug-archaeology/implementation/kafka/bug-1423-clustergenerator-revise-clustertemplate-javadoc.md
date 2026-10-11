**Commit**: 98a7ce5caa · **Ticket**: KAFKA-18801
**Subsystem**: test-common/test-common-internal-api
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>, David Arthur <mumrah@gmail.com>

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
 * cluster configurations. The test-containing class must define a static method which takes no arguments and
 * returns a list of {@link ClusterConfig}, used to generate multiple cluster configurations.
 * Usage looks something like this:
 * <pre>{@code
 * private static List<ClusterConfig> generator() {
 *     return Collections.singletonList(ClusterConfig.defaultBuilder().build());
 * }
 *
 * @ClusterTemplate("generator")
 * public void testGenerateClusterTemplate(ClusterInstance clusterInstance) {
 *     assertNotNull(clusterInstance.bootstrapServers());
 * }
```

**How to spot**
When reviewing test-common/test-common-internal-api code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18801 remove clustergenerator and revise clustertemplate javadoc (#18907).

**Test**
Test modified in ClusterGenerator.java covering the corrected behaviour.
