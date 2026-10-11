**Commit**: d793e3779b · **Ticket**: KAFKA-19678
**Subsystem**: streams/src
**Category**: Resource leak

**Root cause**
This PR changes the approach for handling the case when there are no
open iterators for the `oldest-iterator-open-since-ms` metric. Previously, we would register the metric when the store opens its first
iterator, then deregister the metric when it closes the last remaining
open iterator. Under heavy load this process can cause performance
degradation as detailed in the Jira ticket
https://issues.apache.org/jira/browse/KAFKA-19678. This PR updates the
handling of the metric to register and deregister on store opening and
closing respectively, and handle the case of current open iterators by
returning a `0` to indicate this state.

**Before/After**
```java
// Before
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

// After
import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.LongAdder;
```

**How to spot**
When reviewing streams/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19678: refactor to reset timestamp to zero when no open iterators (#21091).

**Test**
Test modified in StateStoreMetrics.java covering the corrected behaviour.
