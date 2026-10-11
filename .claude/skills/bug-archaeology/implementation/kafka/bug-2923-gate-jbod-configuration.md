**Commit**: 7c0fff7c36 · **Ticket**: KAFKA-16606
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Support for multiple log directories in KRaft exists from
MetataVersion 3.7-IV2. When migrating a ZK broker to KRaft, we already check that
the IBP is high enough before allowing the broker to startup. With KIP-584 and KIP-778, Brokers in KRaft mode do not require
the IBP configuration - the configuration is deprecated. In KRaft mode inter.broker.protocol.version defaults to
MetadataVersion.MINIMUM_KRAFT_VERSION (IBP_3_0_IV1).

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
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16606 gate jbod configuration on 3.7-iv2 (#15834).

**Test**
Test modified in MetadataVersionConfigValidatorTest.java covering the corrected behaviour.
