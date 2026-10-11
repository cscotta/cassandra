**Commit**: e9eb75fc78 · **Ticket**: KAFKA-20194
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
By default, the DSL should expose it's state-stores as ts-stores, as
long as header format is not enabled; otherwise, it would be a backward
incompatible change. This PR ensures that the builders are creating the correct state stores,
depending on the format, and we insert an adaptor to allow the DSL
Processors to work only against headers-store interface. Reviewers: Bill Bejeck <bill@confluent.io>, TengYao Chi
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
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20194: ensure backward compatibility (#21904).

**Test**
Test modified in PapiDslIntegrationTest.java covering the corrected behaviour.
