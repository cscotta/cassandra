**Commit**: 574516dcbf · **Ticket**: KAFKA-19309
**Subsystem**: examples/src
**Category**: Resource leak

**Root cause**
This pull request introduces a new example application,
`TransactionalClientDemo`, which demonstrates how to use Kafka's
transactional capabilities for exactly-once processing semantics. The
application consumes messages from an input topic, processes them to
generate word count statistics, and produces the results to an output
topic. It also includes robust error handling and transaction
management. ### Key Changes:
* Added `TransactionalClientDemo` class to demonstrate a transactional
Kafka client application.

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
When reviewing examples/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19309 : add transaction client template code in kafka examples (#15913).

**Test**
No test added.
