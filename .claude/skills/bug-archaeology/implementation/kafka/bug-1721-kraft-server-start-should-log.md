**Commit**: a52aedd6ff · **Ticket**: KAFKA-18388
**Subsystem**: raft/config
**Category**: Logic error in condition

**Root cause**
Reviewers: TengYao Chi <kitingiao@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
if [ "x$KAFKA_LOG4J_OPTS" = "x" ]; then
    echo "DEPRECATED: using log4j 1.x configuration. To use log4j 2.x configuration, run with: 'export KAFKA_LOG4J_OPTS=\"-Dlog4j.configurationFile=file:$base_dir/../config/kraft-log4j2.yml\"'"
    export KAFKA_LOG4J_OPTS="-Dlog4j.configuration=file:$base_dir/../config/kraft-log4j.properties"
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#

// After
if [ -z "$KAFKA_LOG4J_OPTS" ]; then
    export KAFKA_LOG4J_OPTS="-Dlog4j2.configurationFile=file:$base_dir/../config/kraft-log4j2.yaml"
```

**How to spot**
When reviewing raft/config code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18388 test-kraft-server-start.sh should use log4j2.yaml (#18370).

**Test**
Test modified in test-kraft-server-start.sh covering the corrected behaviour.
