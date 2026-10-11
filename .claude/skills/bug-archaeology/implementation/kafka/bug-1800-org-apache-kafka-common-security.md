**Commit**: 346e5dc322 · **Ticket**: KAFKA-18293
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

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
                        <li>The <code>org.apache.kafka.common.security.oauthbearer.secured.OAuthBearerLoginCallbackHandler</code> class was removed.
                            Please use the <code>org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginCallbackHandler</code> class instead.
                        </li>
                        <li>The <code>org.apache.kafka.common.security.oauthbearer.secured.OAuthBearerValidatorCallbackHandler</code> class was removed.

```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18293 remove `org.apache.kafka.common.security.oauthbearer.secured.oauthbearerlogincallbackhandler` and `org.apach.

**Test**
No test added.
