**Commit**: f1d6549679 · **Ticket**: KAFKA-17706
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Reviewers: David Arthur <mumrah@gmail.com>

**Before/After**
```java
// Before
    sourceSets = []
    sourceSets = []

// After
    configProperties = checkstyleConfigProperties("import-control-test-common.xml")
    configProperties = checkstyleConfigProperties("import-control-test-common-api.xml")
<!DOCTYPE import-control PUBLIC
        "-//Puppy Crawl//DTD Import Control 1.1//EN"
        "http://www.puppycrawl.com/dtds/import_control_1_1.dtd">
<!--
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements.  See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17706 allow all imports for test-common and test-common-api (#17378).

**Test**
Test modified in import-control-test-common-api.xml covering the corrected behaviour.
