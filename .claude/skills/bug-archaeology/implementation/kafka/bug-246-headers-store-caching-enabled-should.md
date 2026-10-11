**Commit**: 579a4e0e2d · **Ticket**: KAFKA-20219
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
For headers-store with caching enabled, we want to keep IQv2 disabled
until we implement IQv2 across the board. Thus, we need to introduce
CachingKeyValueStoreWithHeaders and just forward any query to the
underlying store, instead of re-using the existing query implementation
on CachingKeyValueStore. Reviewers: Matthias J. Sax <matthias@confluent.io>

---------

Co-authored-by: Matthias J.

**Before/After**
```java
// Before
        return new CachingKeyValueStore(inner, CachingKeyValueStore.CacheType.TIMESTAMPED_KEY_VALUE_STORE_WITH_HEADERS);
    @Test
    public void shouldReturnUnknownQueryTypeForKeyQueryOnHeadersStore() {

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
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20219: headers store with caching enabled should reject queries (#21941).

**Test**
Test modified in TimestampedKeyValueStoreBuilderWithHeadersTest.java covering the corrected behaviour.
