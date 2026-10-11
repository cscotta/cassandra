**Commit**: 77d1641673 · **Ticket**: KAFKA-20212
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Enables RocksDBStore to manage offsets. This PR adds a new ColumnFamily for storing offsets committed by Kafka
Streams. Unit tests have been added to verify the new behavior. Reviewers: Nick Telford <nick.telford@gmail.com>, Bill
 Bejeck<bbejeck@apache.org>

---------

Co-authored-by: Bill Bejeck <bbejeck@gmail.com>

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
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20212: manage offsets in rocksdbstore (#21578).

**Test**
Test modified in AbstractColumnFamilyAccessorTest.java covering the corrected behaviour.
