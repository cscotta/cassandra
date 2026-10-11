**Commit**: 749c2d91d5 · **Ticket**: KAFKA-19609
**Subsystem**: checkstyle/import-control-transaction-coordinator.xml
**Category**: Logic error in condition

**Root cause**
This PR migrates the `TransactionLogTest` from Scala to Java for better
consistency with the rest of the test suite and to simplify future
maintenance. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
  private[transaction] def keyToBytes(transactionalId: String): Array[Byte] = {
  private[transaction] def valueToBytes(txnMetadata: TxnTransitMetadata,
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

// After
            <allow pkg="kafka.coordinator.transaction" />
  def keyToBytes(transactionalId: String): Array[Byte] = {
  def valueToBytes(txnMetadata: TxnTransitMetadata,
```

**How to spot**
When reviewing checkstyle/import-control-transaction-coordinator.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19609 move transactionlogtest to transaction-coordinator module (#20460).

**Test**
Test modified in TransactionLogTest.scala covering the corrected behaviour.
