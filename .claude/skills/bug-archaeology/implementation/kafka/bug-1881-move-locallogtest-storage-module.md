**Commit**: e255433374 · **Ticket**: KAFKA-18162
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,

// After
    /**
     * Create a random log directory in the format <string>-<int> used for Kafka partition logs.
     * It is the responsibility of the caller to set up a shutdown hook for deletion of the directory.
     */
    public static File randomPartitionLogDir(File parentDir) {
        int attempts = 1000;
        while (attempts > 0) {
            File f = new File(parentDir, "kafka-" + RANDOM.nextInt(1000000));
            if (f.mkdir()) {
                f.deleteOnExit();
                return f;
            }
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18162 move locallogtest to storage module (#18057).

**Test**
Test modified in TestUtils.java covering the corrected behaviour.
