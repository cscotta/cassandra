**Commit**: 645eefe95a · **Ticket**: KAFKA-17007
**Subsystem**: connect/mirror-client
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        return other != null && toString().equals(other.toString());

// After
        return other instanceof SourceAndTarget && toString().equals(other.toString());
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
```

**How to spot**
When reviewing connect/mirror-client code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17007 fix sourceandtarget#equal (#16416).

**Test**
Test modified in SourceAndTargetTest.java covering the corrected behaviour.
