**Commit**: e05b0e68e4 · **Ticket**: KAFKA-18915
**Subsystem**: checkstyle/import-control-clients-integration-tests.xml
**Category**: Logic error in condition

**Root cause**
Reviewers: Jhen-Yung Hsu <jhenyunghsu@gmail.com>, TengYao Chi <kitingiao@gmail.com>, Ken Huang <s7133700@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work f

// After
  <allow pkg="org.junit" />
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
When reviewing checkstyle/import-control-clients-integration-tests.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18915 rewrite adminclientrebootstraptest to cover the current scenario (#19187).

**Test**
Test modified in import-control-clients-integration-tests.xml covering the corrected behaviour.
