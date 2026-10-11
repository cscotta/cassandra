**Commit**: 3c378dab7d · **Ticket**: KAFKA-19647
**Subsystem**: streams/integration-tests
**Category**: Logic error in condition

**Root cause**
In KAFKA-19570 we implemented offline migration between groups, that is,
the following integration test or system test should be possible:

Test A:

 - Start a streams application with classic protocol, process up to a
certain offset and commit the offset and shut down. - Start the same
streams application with streams protocol (same app ID!). - Make sure
that the offsets before the one committed in the first run are not
reprocessed in the second run. Test B:

 - Start a streams application with streams protocol, process up to a
certain offset and commit the offset and shut down.

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
When reviewing streams/integration-tests code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19647: implement integration test for offline migration (#20412).

**Test**
Test modified in RebalanceProtocolMigrationIntegrationTest.java covering the corrected behaviour.
