**Commit**: 86b552835c · **Ticket**: KAFKA-18026
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Because of how we have to wrap StoreFactory and StoreBuilder layers on top of each other for various parts of the topology building process, we need to make sure both of these are capable of configuration and will check for & delegate to an underlying layer if it exists

Reviewers: Guozhang Wang <guozhang.wang.us@gmail.com>

**Before/After**
```java
// Before
public interface StoreFactory {

    default void configure(final StreamsConfig config) {
        // do nothing
    }
    class FactoryWrappingStoreBuilder<T extends StateStore> implements StoreBuilder<T> {

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
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18026: kip-1112, configure all storebuilder & storefactory layers (#18345).

**Test**
No test added.
