**Commit**: 21b7bb2265 · **Ticket**: KAFKA-18264
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Yung <yungyung7654321@gmail.com>, Mickael Maison <mickael.maison@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
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

// After
public class NotLeaderOrFollowerException extends InvalidMetadataException {
    //   2) `log` instance should not be null here as that would have been caught earlier with NotLeaderOrFollowerException or ReplicaNotAvailableException.
                        <li>The <code>org.apache.kafka.common.errors.NotLeaderForPartitionException</code> class was removed. The <code>org.apache.kafka.common.errors.NotLeaderOrFollowerException</code> is returned
                            if a request could not be processed because the broker is not the leader or follower for a topic partition.
                        </li>
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18264 remove notleaderforpartitionexception (#18211).

**Test**
No test added.
