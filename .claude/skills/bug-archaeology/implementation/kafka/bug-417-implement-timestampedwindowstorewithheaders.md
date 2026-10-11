**Commit**: 7ed35ab26b · **Ticket**: KAFKA-20134
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
This PR adds required classes or modifies the existing ones to build the
`TimestampedWindowStoreWithHeaders` introduced in KIP-1271. Reviewers: Alieh Saeedi <asaeedi@confluent.io>, Matthias J. Sax
 <matthias@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20134: implement timestampedwindowstorewithheaders (5/n) (#21497).

**Test**
Test modified in TimestampedWindowStoreWithHeadersTest.java covering the corrected behaviour.
