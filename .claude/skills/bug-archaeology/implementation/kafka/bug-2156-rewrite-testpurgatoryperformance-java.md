**Commit**: 2ff13976ab · **Ticket**: KAFKA-17568
**Subsystem**: checkstyle/import-control-jmh-benchmarks.xml
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,

// After
    <allow pkg="org.apache.commons"/>
    <allow pkg="joptsimple"/>
```

**How to spot**
When reviewing checkstyle/import-control-jmh-benchmarks.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17568 rewrite testpurgatoryperformance by java (#17246).

**Test**
Test modified in TestPurgatoryPerformance.scala covering the corrected behaviour.
