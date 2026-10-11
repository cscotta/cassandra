**Commit**: 56591d2d07 · **Ticket**: KAFKA-19090
**Subsystem**: core/src
**Category**: Race condition / missing synchronization

**Root cause**
Rewrite these classes in Java and move them to the server module

Reviewers: Mickael Maison <mickael.maison@gmail.com>

**Before/After**
```java
// Before
      new DelayedFuturePurgatory(purgatoryName = "AlterAcls", brokerId = config.nodeId)
        val createResults = auth.createAcls(request.context, validBindings.asJava).asScala.map(_.toCompletableFuture)
            val result = errorResults.getOrElse(acl, createResults(validBindings.indexOf(acl)).get)
        alterAclsPurgatory.tryCompleteElseWatch(config.connectionsMaxIdleMs, createResults, sendResponseCallback)
          .asScala.map(_.toCompletableFuture).toList
          val filterResults = deleteResults.map(_.get).map(DeleteAclsResponse.filterResult).asJava
        alterAclsPurgatory.tryCompleteElseWatch(config.connectionsMaxIdleMs, deleteResults, sendResponseCallback)
/**
  * Licensed to the Apache Software Foundation (ASF) under one or more
  * contributor license agreements.  See the NOTICE file distributed with
  * this work for additional information regarding copyright ownership.
  * The ASF licenses this file to You under the Apache License, Version 2.0

// After
import org.apache.kafka.server.purgatory.DelayedFuturePurgatory
import java.util.stream.Collectors
      new DelayedFuturePurgatory("AlterAcls", config.nodeId)
        val createResults = auth.createAcls(request.context, validBindings.asJava).stream().map(_.toCompletableFuture).toList
            val result = errorResults.getOrElse(acl, createResults.get(validBindings.indexOf(acl)).get)
        alterAclsPurgatory.tryCompleteElseWatch(config.connectionsMaxIdleMs, createResults, () => sendResponseCallback())
          .stream().map(_.toCompletableFuture).toList
          val filterResults: util.List[DeleteAclsFilterResult] = deleteResults.stream().map(_.get)
            .map(DeleteAclsResponse.filterResult).collect(Collectors.toList())
        alterAclsPurgatory.tryCompleteElseWatch(config.connectionsMaxIdleMs, deleteResults, () => sendResponseCallback())
```

**How to spot**
When reviewing core/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19090: move delayedfuture and delayedfuturepurgatory to server module (#19390).

**Test**
Test modified in DelayedFutureTest.scala covering the corrected behaviour.
