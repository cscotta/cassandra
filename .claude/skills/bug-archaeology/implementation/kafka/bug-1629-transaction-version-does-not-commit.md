**Commit**: d3b4c1bdf4 · **Ticket**: KAFKA-18401
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Fix the issue where producer.commitTransaction under transaction version 2 throws error if no partition or offset is added to transaction. The solution is to avoid sending the endTxnRequest unless producer.send or producer.sendOffsetsToTransaction is triggered. Reviewers: Justine Olshan <jolshan@confluent.io>

**Before/After**
```java
// Before
        if (nextRequestHandler.isEndTxn() && (!isTransactionV2Enabled() && !transactionStarted)) {
                log.debug("Not sending EndTxn for completed transaction since no partitions " +
                        "or offsets were successfully added");
/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0

// After
            transactionStarted = true;
                transactionStarted = true;
        if (nextRequestHandler.isEndTxn() && !transactionStarted) {
                if (isTransactionV2Enabled) {
                    log.debug("Not sending EndTxn for completed transaction since no send " +
                            "or sendOffsetsToTransaction were triggered");
                } else {
                    log.debug("Not sending EndTxn for completed transaction since no partitions " +
                            "or offsets were successfully added");
                }
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18401: transaction version 2 does not support commit transaction without records (#18448).

**Test**
Test modified in ProducerIdsIntegrationTest.scala covering the corrected behaviour.
