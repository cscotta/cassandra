**Commit**: 5a4590c020 · **Ticket**: KAFKA-18338
**Subsystem**: test-common/test-common-api
**Category**: Logic error in condition

**Root cause**
Reviewers: TengYao Chi <kitingiao@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
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
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

// After
# contributor license agreements. See the NOTICE file distributed with
# the License. You may obtain a copy of the License at
Configuration:
  Properties:
    Property:
      - name: "logPattern"
        value: "[%d] %p %m (%c:%L)%n"
  Appenders:
    Console:
      name: STDOUT
      PatternLayout:
        pattern: "${logPattern}"
```

**How to spot**
When reviewing test-common/test-common-api code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18338 add log4j.yaml to test-common-api and remove unsed log4j.properties from test-common (#18294).

**Test**
Test modified in log4j.properties covering the corrected behaviour.
