**Commit**: 50b6953661 · **Ticket**: KAFKA-18122
**Subsystem**: trogdor/src
**Category**: Logic error in condition

**Root cause**
Added ShareConsumeBenchSpec and ShareConsumeBenchWorker similar to ConsumeBenchSpec/ConsumeBenchWorker. This will help us run trogdor workloads for share consumers as well. Added a sample json workload running 5 share consumers. Reviewers: Andrew Schofield <aschofield@confluent.io>, Manikumar Reddy <manikumar.reddy@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements.  See the NOTICE file distributed with
// this work for additional information regarding copyright ownership.
// The ASF licenses this file to You under the Apache License, Version 2.0
// (the "License"); you may not use this file except in compliance with
// the License.  You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
```

**How to spot**
When reviewing trogdor/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18122 : added support for shareconsumebenchworker (#17984).

**Test**
Test modified in simple_share_consume_bench_spec.json covering the corrected behaviour.
