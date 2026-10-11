**Commit**: 35420eb11b · **Ticket**: KAFKA-18684
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
Introduced two new exception classes to the Kafka error handling framework:

ApplicationRecoverableException: This exception signals that the error is recoverable, but the producer needs to be restarted. It helps in scenarios where recovery actions (like re-balancing or restoring from checkpoints) are needed. RefreshRetriableException: This exception occurs when metadata is outdated or invalid and needs to be refreshed before retrying the request. It helps handle retries that depend on updated metadata.

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
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18684: add base exception classes (#18871).

**Test**
No test added.
