**Commit**: 239708f52e · **Ticket**: KAFKA-18518
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
This commit adds a processor named
StreamsRebalanceEventsProcessor that handles the rebalance
events sent from the background thread of the async
consumer to the stream thread when an task
assignment changes. It also adds the corresponding rebalance
events. Additionally, this commit adds StreamsRebalanceData that
maintains the data that is exchanges for the Streams rebalance
protocol. All of these are used by the Streams heartbeat request manager
and the Streams membership manager that will be added in a future
commit.

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
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18518: add processor to handle rebalance events (#18527).

**Test**
Test modified in StreamsRebalanceDataTest.java covering the corrected behaviour.
