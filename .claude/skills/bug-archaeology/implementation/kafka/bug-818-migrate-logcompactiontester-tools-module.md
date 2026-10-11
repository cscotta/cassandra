**Commit**: 4a5562c341 · **Ticket**: KAFKA-19306
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
jira: [KAFKA-19306](https://issues.apache.org/jira/browse/KAFKA-19306)

log
```
Producing 1000000 messages..to topics
log-cleaner-test-849894102467800668-0
Logging produce requests to
/tmp/kafka-log-cleaner-produced-6049271649847384547.txt
Sleeping for 20seconds... Consuming messages... Logging consumed messages to
/tmp/kafka-log-cleaner-consumed-7065252868189829937.txt
1000000 rows of data produced, 120176 rows of data consumed (88.0%
reduction). De-duplicating and validating output files...

**Before/After**
```java
// Before
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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,

// After
// (no additions)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19306 migrate logcompactiontester to tools module (#19905).

**Test**
Test modified in LogCompactionTester.scala covering the corrected behaviour.
