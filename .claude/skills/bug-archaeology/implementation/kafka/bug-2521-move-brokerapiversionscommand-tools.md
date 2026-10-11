**Commit**: 28cd7136fb · **Ticket**: KAFKA-16324
**Subsystem**: bin/kafka-broker-api-versions.sh
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
exec $(dirname $0)/kafka-run-class.sh kafka.admin.BrokerApiVersionsCommand "$@"
%~dp0kafka-run-class.bat kafka.admin.BrokerApiVersionsCommand %*
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
exec $(dirname $0)/kafka-run-class.sh org.apache.kafka.tools.BrokerApiVersionsCommand "$@"
%~dp0kafka-run-class.bat org.apache.kafka.tools.BrokerApiVersionsCommand %*
```

**How to spot**
When reviewing bin/kafka-broker-api-versions.sh code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16324 move brokerapiversionscommand to tools (#16850).

**Test**
Test modified in BrokerApiVersionsCommandTest.scala covering the corrected behaviour.
