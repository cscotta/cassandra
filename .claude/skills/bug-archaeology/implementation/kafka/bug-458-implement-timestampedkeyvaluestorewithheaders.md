**Commit**: 060650ed21 · **Ticket**: KAFKA-20132
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
This PR add RocksDBTimestampedStoreWithHeaders (and the corresponding
unit test) for the TimestampedKeyValueStoreWithHeaders introduced in KIP-1271. Reviewers: TengYao Chi <frankvicky@apache.org>, Matthias J. Sax
 <matthias@confluent.io>

**Before/After**
```java
// Before
    private static final byte[] TIMESTAMPED_VALUES_COLUMN_FAMILY_NAME = "keyValueWithTimestamp".getBytes(StandardCharsets.UTF_8);

// After
    static final byte[] TIMESTAMPED_VALUES_COLUMN_FAMILY_NAME = "keyValueWithTimestamp".getBytes(StandardCharsets.UTF_8);
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
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20132: implement timestampedkeyvaluestorewithheaders (2/n) (#21446).

**Test**
Test modified in RocksDBTimestampedStoreWithHeadersTest.java covering the corrected behaviour.
