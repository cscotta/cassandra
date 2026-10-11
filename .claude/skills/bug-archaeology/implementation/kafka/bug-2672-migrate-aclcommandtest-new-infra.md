**Commit**: 3e2de237be · **Ticket**: KAFKA-17021
**Subsystem**: checkstyle/import-control-core.xml
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    <allow pkg="org.apache.kafka.clients.admin" />

// After
    <allow pkg="kafka.admin"/>
    <allow pkg="kafka.security.authorizer"/>
    <allow pkg="org.apache.kafka.metadata.authorizer"/>
    <allow pkg="org.apache.log4j"/>
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
```

**How to spot**
When reviewing checkstyle/import-control-core.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17021 migrate aclcommandtest to new test infra (#16500).

**Test**
Test modified in AclCommandTest.java covering the corrected behaviour.
