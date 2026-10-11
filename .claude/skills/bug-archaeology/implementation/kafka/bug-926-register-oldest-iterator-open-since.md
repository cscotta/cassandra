**Commit**: 4387132926 · **Ticket**: KAFKA-19398
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
The metric for oldest-iterator-open-since-ms might report a null value
if there is not open iterator. This PR changes the behavior to dynamically register/deregister the
entire metric instead of allowing it to return a null value. Reviewers: Bill Bejeck <bbejeck@apache.org>

**Before/After**
```java
// Before
    public <T> void addStoreLevelMutableMetric(final String taskId,
                                               final String metricsScope,
                                               final String storeName,
                                               final String name,
                                               final String description,
                                               final RecordingLevel recordingLevel,
                                               final Gauge<T> valueProvider) {

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
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19398: (de)register oldest-iterator-open-since-ms metric dynamically (#20022).

**Test**
Test modified in StateStoreMetrics.java covering the corrected behaviour.
