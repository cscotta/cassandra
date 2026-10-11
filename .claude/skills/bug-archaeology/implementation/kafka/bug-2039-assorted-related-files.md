**Commit**: 085b27ec6e · **Ticket**: KAFKA-17987
**Subsystem**: bin/zookeeper-security-migration.sh
**Category**: Logic error in condition

**Root cause**
Remove zookeeper files in bin:
- bin/zookeeper-security-migration.sh
- bin/zookeeper-server-start.sh
- bin/zookeeper-server-stop.sh
- bin/zookeeper-shell.sh

Remove files used to configure Kafka in zookeeper mode in config:
- config/server.properties
- config/zookeeper.properties

Remove ZK references from all remaining Kafka configuration files. Remove ZK references from all log4j.properties files. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
#!/bin/bash
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,

// After
// (no additions)
```

**How to spot**
When reviewing bin/zookeeper-security-migration.sh code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17987 remove assorted zk-related files (#17768).

**Test**
Test modified in log4j.properties covering the corrected behaviour.
