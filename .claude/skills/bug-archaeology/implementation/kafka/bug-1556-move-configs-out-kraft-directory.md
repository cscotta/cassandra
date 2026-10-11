**Commit**: 7e86bd8281 · **Ticket**: KAFKA-18229
**Subsystem**: README.md
**Category**: Logic error in condition

**Root cause**
Reviewers: Mickael Maison <mickael.maison@gmail.com>, Ismael Juma <ismael@juma.me.uk>, José Armando García Sancio <jsancio@apache.org>

**Before/After**
```java
// Before
    ./bin/kafka-storage.sh format --standalone -t $KAFKA_CLUSTER_ID -c config/kraft/reconfig-server.properties
    ./bin/kafka-server-start.sh config/kraft/reconfig-server.properties
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

// After
    ./bin/kafka-storage.sh format --standalone -t $KAFKA_CLUSTER_ID -c config/server.properties
    ./bin/kafka-server-start.sh config/server.properties
```

**How to spot**
When reviewing README.md code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18229: move configs out of "kraft" directory (#18389).

**Test**
No test added.
