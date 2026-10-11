**Commit**: e64a324068 · **Ticket**: KAFKA-19884
**Subsystem**: clients/clients-integration-tests
**Category**: Resource leak

**Root cause**
Ensure that `close()` takes at least `DEFAULT_FETCH_MAX_WAIT_MS`
milliseconds to close, and ensure that `close(CloseOptions)` with a
short timeout closes before `DEFAULT_FETCH_MAX_WAIT_MS`. Reviewers: Lianet Magrans <lmagrans@confluent.io>, TengYao Chi
 <frankvicky@apache.org>

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
When reviewing clients/clients-integration-tests code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19884: add tests to ensure consumer.close() timeouts work as documented (#21037).

**Test**
Test modified in PlaintextConsumerCloseTest.java covering the corrected behaviour.
