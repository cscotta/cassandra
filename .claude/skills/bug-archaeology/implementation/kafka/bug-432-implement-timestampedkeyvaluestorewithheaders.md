**Commit**: b603b5306e · **Ticket**: KAFKA-20132
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
This PR implements the upgrade integration tests for
`TimestampedKeyValueStoreWithHeaders` introduced in kip-1271.

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
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20132: implement timestampedkeyvaluestorewithheaders (n/n) (#21529).

**Test**
Test modified in HeadersStoreUpgradeIntegrationTest.java covering the corrected behaviour.
