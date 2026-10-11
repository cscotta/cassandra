**Commit**: 2fdb687029 · **Ticket**: KAFKA-19082
**Subsystem**: clients/src
**Category**: Wrong serialization / deserialization

**Root cause**
This is part of the client side changes required to enable 2PC for
KIP-939

New KafkaProducer.PreparedTxnState class is going to be defined as
following:  ```  static public class PreparedTxnState {    public String
toString();    public PreparedTxnState(String serializedState); public
PreparedTxnState();  }  ```  The objects of this class can serialize to
/ deserialize from a string value and can be written to / read from a
database. The implementation is going to store producerId and epoch in
the format **producerId:epoch**

Reviewers: Artem Livshits <alivshits@confluent.io>, Justine Olshan
 <jolshan@confluent.io>

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
When reviewing clients/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19082: [2/4] add preparedtxnstate class to kafka producer (kip-939) (#19470).

**Test**
Test modified in PreparedTxnStateTest.java covering the corrected behaviour.
