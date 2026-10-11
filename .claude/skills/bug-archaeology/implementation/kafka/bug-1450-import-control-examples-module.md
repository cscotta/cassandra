**Commit**: a6972db660 · **Ticket**: KAFKA-18658
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    configProperties = checkstyleConfigProperties("import-control-core.xml")

// After
    configProperties = checkstyleConfigProperties("import-control-examples.xml")
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
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18658 add import control for examples module (#18812).

**Test**
No test added.
