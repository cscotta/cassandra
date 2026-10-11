**Commit**: 78bc9486be · **Ticket**: KAFKA-20013
**Subsystem**: docs/README.md
**Category**: Logic error in condition

**Root cause**
Convert HTML docs to Markdown. These match the version of the docs
currently in kafka-site

Reviewers: David Arthur <mumrah@gmail.com>

**Before/After**
```java
// Before
# Docs

The documentation needs to run with [kafka-site](https://github.com/apache/kafka-site).
Please check the README in the kafka-site repository to preview documentation locally.
<!--
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements.  See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at


// After
---
title: AK 4.3.X
description: Documentation for AK 4.3.X
weight:
tags: ['kafka', 'docs']
aliases:
keywords:
type: docs
---

<!--
 Licensed to the Apache Software Foundation (ASF) under one or more
```

**How to spot**
When reviewing docs/README.md code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20013 markdown docs for 4.3.x / trunk (#21195).

**Test**
Test modified in testing.html covering the corrected behaviour.
