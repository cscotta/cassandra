**Commit**: 23e77ed2d4 · **Ticket**: KAFKA-18374
**Subsystem**: server-common/src
**Category**: Wrong serialization / deserialization

**Root cause**
Reviewers: TengYao Chi <kitingiao@gmail.com>, TaiJuWu <tjwu1217@gmail.com>, Christo Lolov <lolovc@amazon.com>, PoAn Yang <payang@apache.org>, Chia-Ping Tsai <chia7712@gmail.com>

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
// (no additions)
```

**How to spot**
When reviewing server-common/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18374 remove encryptingpasswordencoder, cipherparamsencoder, gcmparamsencoder, ivparamsencoder, and the unused sta.

**Test**
No test added.
