**Commit**: 01e9918530 · **Ticket**: KAFKA-16814
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
When starting up kafka logManager, we'll check stray replicas to avoid some corner cases. But this check might cause broker unable to startup if partition.metadata is missing because when startup kafka, we load log from file, and the topicId of the log is coming from partition.metadata file. So, if partition.metadata is missing, the topicId will be None, and the LogManager#isStrayKraftReplica will fail with no topicID error. The partition.metadata missing could be some storage failure, or another possible path is unclean shutdown after topic is created in the replica, but before data is flushed into partition.metadata file.

**Before/After**
```java
// Before
    val topicId = log.topicId.getOrElse {
      throw new RuntimeException(s"The log dir $log does not have a topic ID, " +
        "which is not allowed when running in KRaft mode.")

// After
    if (log.topicId.isEmpty) {
      // Missing topic ID could result from storage failure or unclean shutdown after topic creation but before flushing
      // data to the `partition.metadata` file. And before appending data to the log, the `partition.metadata` is always
      // flushed to disk. So if the topic ID is missing, it mostly means no data was appended, and we can treat this as
      // a stray log.
      info(s"The topicId does not exist in $log, treat it as a stray log")
      return true
    val topicId = log.topicId.get
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16814 kraft broker cannot startup when `partition.metadata` is missing (#16165).

**Test**
Test modified in LogManagerIntegrationTest.java covering the corrected behaviour.
