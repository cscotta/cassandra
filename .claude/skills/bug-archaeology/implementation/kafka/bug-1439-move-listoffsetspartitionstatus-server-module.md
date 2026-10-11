**Commit**: 9494bebee6 · **Ticket**: KAFKA-18728
**Subsystem**: checkstyle/import-control-server.xml
**Category**: Logic error in condition

**Root cause**
Reviewers: Kamal Chandraprakash <kamal.chandraprakash@gmail.com>

**Before/After**
```java
// Before
    status.completed = status.futureHolderOpt.isEmpty
      status.responseOpt = Some(buildErrorResponse(Errors.REQUEST_TIMED_OUT, topicPartition.partition()))
        new ListOffsetsTopicResponse().setName(topic).setPartitions(status.values.flatMap(s => s.responseOpt).toList.asJava)
                  .getOrElse(Errors.NONE)
                if (status.lastFetchableOffset.isDefined && found.offset >= status.lastFetchableOffset.get) {
                  if (status.maybeOffsetsError.isDefined) {
            status.responseOpt = Some(response)
            status.completed = true
/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.

// After
    <allow pkg="org.apache.kafka.storage.internals.log" />
import org.apache.kafka.server.ListOffsetsPartitionStatus
    status.completed(status.futureHolderOpt.isEmpty)
      status.responseOpt(Optional.of(buildErrorResponse(Errors.REQUEST_TIMED_OUT, topicPartition.partition())))
        new ListOffsetsTopicResponse().setName(topic).setPartitions(status.values.flatMap(s => Some(s.responseOpt.get())).toList.asJava)
                  .orElse(Errors.NONE)
                if (status.lastFetchableOffset.isPresent && found.offset >= status.lastFetchableOffset.get) {
                  if (status.maybeOffsetsError.isPresent) {
            status.responseOpt(Optional.of(response))
            status.completed(true)
```

**How to spot**
When reviewing checkstyle/import-control-server.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18728 move listoffsetspartitionstatus to server module (#18807).

**Test**
Test modified in DelayedRemoteListOffsetsTest.scala covering the corrected behaviour.
