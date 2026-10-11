**Commit**: b22c7d5b5c · **Ticket**: KAFKA-17833
**Subsystem**: checkstyle/import-control-core.xml
**Category**: Logic error in condition

**Root cause**
Reviewers: Mickael Maison <mickael.maison@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
    <allow pkg="org.apache.kafka.clients"/>
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
```

**How to spot**
When reviewing checkstyle/import-control-core.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17833: convert describeauthorizedoperationstest to use kraft (#18252).

**Test**
Test modified in DescribeAuthorizedOperationsTest.java covering the corrected behaviour.
