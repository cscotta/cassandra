**Commit**: cf07e6b1f6 · **Ticket**: KAFKA-18074
**Subsystem**: docs/compatibility-summary.html
**Category**: Logic error in condition

**Root cause**
Add new section for Kafka 4.0 compatibility metrics for user, so user
can check server and client in this section. Reviewers: Divij Vaidya <diviv@amazon.com>, Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
      <h3 class="anchor-heading"><a id="docker" class="anchor-link"></a><a href="#docker">1.7 Docker</a></h3>

// After
<!--
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements.  See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
```

**How to spot**
When reviewing docs/compatibility-summary.html code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18074: add kafka client compatibility matrix (#18091).

**Test**
No test added.
