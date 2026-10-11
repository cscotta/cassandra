**Commit**: 8fce97b2c3 · **Ticket**: KAFKA-19144
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
This PR moves `DelayedProduce` to the server module. One notable change
is that the type of the `responseCallback` parameter in
`ReplicaManager#appendRecords()` has been changed to a Java `Map`. Other
related type changes have been made accordingly. Reviewers: Ken Huang <s7133700@gmail.com>, PoAn Yang
 <payang@apache.org>, TengYao Chi <frankvicky@apache.org>, DL1231
 <53332773+DL1231@users.noreply.github.com>, Kuan-Po Tseng
 <brandboat@gmail.com>, Christo Lolov <lolovc@amazon.com>, Chia-Ping
 Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.server.purgatory.{DelayedDeleteRecords, DelayedOperationPurgatory, TopicPartitionOperationKey}
    def removeFromCacheCallback(responses: collection.Map[TopicIdPartition, PartitionResponse]): Unit = {
      responses.foreachEntry { (topicPartition, response) =>
    def updateCacheCallback(responseStatus: collection.Map[TopicIdPartition, PartitionResponse]): Unit = {
      if (responseStatus.size != 1 || !responseStatus.contains(transactionStateTopicIdPartition))
      val status = responseStatus(transactionStateTopicIdPartition)
/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with

// After
import org.apache.kafka.server.purgatory.{DelayedDeleteRecords, DelayedOperationPurgatory, DelayedProduce, TopicPartitionOperationKey}
import java.util
    def removeFromCacheCallback(responses: util.Map[TopicIdPartition, PartitionResponse]): Unit = {
      responses.forEach { (topicPartition, response) =>
    def updateCacheCallback(responseStatus: util.Map[TopicIdPartition, PartitionResponse]): Unit = {
      if (responseStatus.size != 1 || !responseStatus.containsKey(transactionStateTopicIdPartition))
      val status = responseStatus.get(transactionStateTopicIdPartition)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19144 move delayedproduce to server module (#19793).

**Test**
Test modified in LocalLeaderEndPointTest.scala covering the corrected behaviour.
