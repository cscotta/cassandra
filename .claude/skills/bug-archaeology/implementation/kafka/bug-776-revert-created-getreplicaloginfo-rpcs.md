**Commit**: 29ce96151c · **Ticket**: KAFKA-18681
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
This reverts commit d86ba7f54af4c8674dc27aa1a2e54647b450ba7a. Reverting since we are planning to change how KIP-966 is implemented. We
should revert this RPC until we have more clarity on how this KIP will
be executed. Reviewers: José Armando García Sancio <jsancio@apache.org>

**Before/After**
```java
// Before
    DELETE_SHARE_GROUP_OFFSETS(ApiMessageType.DELETE_SHARE_GROUP_OFFSETS),
    GET_REPLICA_LOG_INFO(ApiMessageType.GET_REPLICA_LOG_INFO);
            case GET_REPLICA_LOG_INFO:
                return GetReplicaLogInfoRequest.parse(readable, apiVersion);
            case GET_REPLICA_LOG_INFO:
                return GetReplicaLogInfoResponse.parse(readable, version);
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with

// After
    DELETE_SHARE_GROUP_OFFSETS(ApiMessageType.DELETE_SHARE_GROUP_OFFSETS);

```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: minor; revert "kafka-18681: created getreplicaloginfo rpcs (#19664)" (#20371).

**Test**
Test modified in RequestResponseTest.java covering the corrected behaviour.
