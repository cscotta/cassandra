**Commit**: 63229a768c · **Ticket**: KAFKA-16718
**Subsystem**: clients/src
**Category**: Wrong serialization / deserialization

**Root cause**
Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
    ALTER_SHARE_GROUP_OFFSETS(ApiMessageType.ALTER_SHARE_GROUP_OFFSETS);

// After
    ALTER_SHARE_GROUP_OFFSETS(ApiMessageType.ALTER_SHARE_GROUP_OFFSETS),
    DELETE_SHARE_GROUP_OFFSETS(ApiMessageType.DELETE_SHARE_GROUP_OFFSETS);
            case DELETE_SHARE_GROUP_OFFSETS:
                return DeleteShareGroupOffsetsRequest.parse(buffer, apiVersion);
            case DELETE_SHARE_GROUP_OFFSETS:
                return DeleteShareGroupOffsetsResponse.parse(responseBuffer, version);
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
```

**How to spot**
When reviewing clients/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16718 [1/n]: added deletesharegroupoffsets request and response schema (#18927).

**Test**
Test modified in RequestResponseTest.java covering the corrected behaviour.
