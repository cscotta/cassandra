**Commit**: 7863b35064 · **Ticket**: KAFKA-14485
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Move LogCleaner and related classes to storage module and rewrite in
Java. Reviewers: Mickael Maison <mickael.maison@gmail.com>, Jun Rao <junrao@gmail.com>

**Before/After**
```java
// Before
    testImplementation project(':server-common').sourceSets.test.output
      - name: kafka.log.LogCleaner
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

// After
    testImplementation project(':server-common')
            <allow pkg="org.apache.kafka.clients"/>
        <allow pkg="org.apache.kafka.config" />
      - name: org.apache.kafka.storage.internals.log.LogCleaner
        level: INFO
        additivity: false
        AppenderRef:
          ref: CleanerAppender
      - name: org.apache.kafka.storage.internals.log.LogCleaner$CleanerThread
        level: INFO
        additivity: false
        AppenderRef:
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-14485: move logcleaner to storage module (#19387).

**Test**
Test modified in GroupCoordinatorIntegrationTest.scala covering the corrected behaviour.
