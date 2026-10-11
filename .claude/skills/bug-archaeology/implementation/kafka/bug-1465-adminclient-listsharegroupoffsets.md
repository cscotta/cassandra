**Commit**: 7dbed2f6e8 · **Ticket**: KAFKA-16720
**Subsystem**: checkstyle/import-control-group-coordinator.xml
**Category**: Logic error in condition

**Root cause**
Reviewers: Apoorv Mittal <apoorvmittal10@gmail.com>, Sushant Mahajan <smahajan@confluent.io>, Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
    // To do in a follow-up PR
        // To-do
        throw new InvalidRequestException("The method is not yet implemented");

// After
            <allow pkg="org.apache.kafka.server.share.persister"/>
import org.apache.kafka.clients.admin.internals.ListShareGroupOffsetsHandler;
        SimpleAdminApiFuture<CoordinatorKey, Map<TopicPartition, Long>> future = ListShareGroupOffsetsHandler.newFuture(groupSpecs.keySet());
        ListShareGroupOffsetsHandler handler = new ListShareGroupOffsetsHandler(groupSpecs, logContext);
        invokeDriver(handler, future, options.timeoutMs);
        return new ListShareGroupOffsetsResult(future.all());
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
```

**How to spot**
When reviewing checkstyle/import-control-group-coordinator.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: [kafka-16720] adminclient support for listsharegroupoffsets (2/2) (#18671).

**Test**
Test modified in AdminClientTestUtils.java covering the corrected behaviour.
