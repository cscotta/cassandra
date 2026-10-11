**Commit**: cebec91470 · **Ticket**: KAFKA-18080
**Subsystem**: core/src
**Category**: State not cleaned up

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import kafka.utils.{DelayedItem, Logging, Pool}
              currentFetchState.lag, currentFetchState.currentLeaderEpoch, Some(new DelayedItem(delay)),
 * (1) Truncating its log, for example having recently become a follower
 * (2) Delayed, for example due to an error, where we subsequently back off a bit
 * (3) ReadyForFetch, the is the active state where the thread is actively fetching data.
                               delay: Option[DelayedItem],
  def isDelayed: Boolean = delay.exists(_.getDelay(TimeUnit.MILLISECONDS) > 0)
      s", delay=${delay.map(_.delayMs).getOrElse(0)}ms" +
/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.

// After
import kafka.utils.{Logging, Pool}
import org.apache.kafka.common.utils.Time
              currentFetchState.lag, currentFetchState.currentLeaderEpoch, Some(delay),
 * (1) Truncating its log, for example, having recently become a follower
 * (2) Delayed, for example, due to an error, where we subsequently back off a bit
 * (3) ReadyForFetch, the active state where the thread is actively fetching data.
                               delay: Option[Long],
  private val dueMs = delay.map(_ + Time.SYSTEM.milliseconds)

  def isDelayed: Boolean = dueMs.exists(_ > Time.SYSTEM.milliseconds)
      s", delay=${delay.getOrElse(0)}ms" +
```

**How to spot**
When reviewing core/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18080 replace delayeditem by long type (#17927).

**Test**
Test modified in ReplicaAlterLogDirsThreadTest.scala covering the corrected behaviour.
