**Commit**: 323eaaca53 · **Ticket**: KAFKA-20256
**Subsystem**: server-common/src
**Category**: Logic error in condition

**Root cause**
* Add `DLQManager` interface (evolving). * Add a NoOp impl for above. * Add java record classes to server as arguments for
`ShareGroupDLQ.enqueue`

Reviewers: Andrew Schofield <aschofield@confluent.io>

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
When reviewing server-common/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20256: add sharegroupdlq interface and some infra classes. (#21640).

**Test**
No test added.
