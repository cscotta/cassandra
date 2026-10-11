**Commit**: 8cfd631264 · **Ticket**: KAFKA-16723
**Subsystem**: bin/kafka-console-share-consumer.sh
**Category**: Logic error in condition

**Root cause**
Added kafka-console-share-consumer.sh which will start a share consumer on a share group. This tool helps to read data from Kafka topics using share groups and outputs it to standard output. Reviewers:  Andrew Schofield <aschofield@confluent.io>, Apoorv Mittal <apoorvmittal10@gmail.com>,  Manikumar Reddy <manikumar.reddy@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
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
```

**How to spot**
When reviewing bin/kafka-console-share-consumer.sh code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16723 : added kafka-console-share-consumer.sh tool. (#16860).

**Test**
Test modified in ConsoleShareConsumerOptionsTest.java covering the corrected behaviour.
