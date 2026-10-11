**Commit**: ffc865c432 · **Ticket**: KAFKA-17291
**Subsystem**: clients/src
**Category**: Incorrect filtering / wrong query result

**Root cause**
Add an integration test for share group list and describe admin operations. Reviewers: Omnia Ibrahim <o.g.h.ibrahim@gmail.com>, Manikumar Reddy <manikumar.reddy@gmail.com>

**Before/After**
```java
// Before
    private Set<AclOperation> validAclOperations(final int authorizedOperations) {
        if (authorizedOperations == MetadataResponse.AUTHORIZED_OPERATIONS_OMITTED) {
            return null;
        }
        return Utils.from32BitField(authorizedOperations)
            .stream()
            .map(AclOperation::fromCode)
            .filter(operation -> operation != AclOperation.UNKNOWN
                && operation != AclOperation.ALL
                && operation != AclOperation.ANY)
            .collect(Collectors.toSet());
    }

// After
import static org.apache.kafka.clients.admin.internals.AdminUtils.validAclOperations;
        if (this == o) return true;
        if (!(o instanceof ShareGroupListing)) return false;
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
```

**How to spot**
When reviewing clients/src code, look for incorrect filtering / wrong query result patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17291: add integration test for share group list and describe (#16920).

**Test**
Test modified in IntegrationTestHarness.scala covering the corrected behaviour.
