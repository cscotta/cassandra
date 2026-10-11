**Commit**: f68a149a18 · **Ticket**: KAFKA-18509
**Subsystem**: core/src
**Category**: Race condition / missing synchronization

**Root cause**
We can rewrite this class from scala to java and move to server-common
module. To maintain backward compatibility, we should keep the logger
name `state.change.logger`. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import kafka.controller.StateChangeLogger
  private val stateChangeLogger = new StateChangeLogger(localBrokerId, inControllerContext = false, None)
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
import org.apache.kafka.logger.StateChangeLogger
  private val stateChangeLogger = new StateChangeLogger(localBrokerId)
import org.apache.kafka.logger.StateChangeLogger
  protected val stateChangeLogger = new StateChangeLogger(localBrokerId)
```

**How to spot**
When reviewing core/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18509 move statechangelogger to server-common module (#20637).

**Test**
No test added.
