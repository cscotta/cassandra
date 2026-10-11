**Commit**: 4c4458c17a · **Ticket**: KAFKA-18799
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.admin.AdminUtils
      val internalTopicsAllowed = request.header.clientId == AdminUtils.ADMIN_CLIENT_ID
/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *

// After
      val internalTopicsAllowed = request.header.clientId == "__admin_client"
```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18799 remove adminutils (#18946).

**Test**
Test modified in AdminRackAwareTest.scala covering the corrected behaviour.
