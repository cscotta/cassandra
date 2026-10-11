**Commit**: ebe1e964ff · **Ticket**: KAFKA-16448
**Subsystem**: streams/src
**Category**: Resource leak

**Root cause**
This PR is part of KAFKA-16448 which aims to bring a ProcessingExceptionHandler to Kafka Streams in order to deal with exceptions that occur during processing. This PR brings ProcessingExceptionHandler interface and default implementations. Co-authored-by: Dabz <d.gasparina@gmail.com>
Co-authored-by: sebastienviale <sebastien.viale@michelin.com>

Reviewer: Bruno Cadonna <cadonna@apache.org>

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
When reviewing streams/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16448: add processingexceptionhandler interface and implementations (#16187).

**Test**
No test added.
