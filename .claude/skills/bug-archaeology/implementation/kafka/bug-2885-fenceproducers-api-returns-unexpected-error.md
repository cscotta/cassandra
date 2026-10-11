**Commit**: 615e6e705c · **Ticket**: KAFKA-16570
**Subsystem**: clients/src
**Category**: Race condition / missing synchronization

**Root cause**
KAFKA-16570 FenceProducers API returns "unexpected error" when successful

* Client handling of ConcurrentTransactionsException as retriable
* Unit test
* Integration test

Reviewers: Chris Egerton <chrise@aiven.io>, Justine Olshan <jolshan@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After
            case CONCURRENT_TRANSACTIONS:
                log.debug("InitProducerId request for transactionalId `{}` failed because of " +
                                "a concurrent transaction. Will retry", transactionalIdKey.idValue);
                return ApiResult.empty();
        assertRetriableError(handler, transactionalId, Errors.CONCURRENT_TRANSACTIONS);
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
```

**How to spot**
When reviewing clients/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16570 fenceproducers api returns "unexpected error" when succes… (#16229).

**Test**
Test modified in FenceProducersHandlerTest.java covering the corrected behaviour.
