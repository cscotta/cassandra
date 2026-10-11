**Commit**: c62942d47b · **Ticket**: KAFKA-19977
**Subsystem**: docs/documentation
**Category**: Logic error in condition

**Root cause**
Add new developer guide page documenting the broker-driven Streams
Rebalance Protocol, including features, configuration, administration,
and architecture. Update navigation links across developer guide pages
to integrate the new section. Some of the pagination links didn't look right. Not sure if it matters
since we anyway are replacing this representation

Reviewers: Matthias J.

**Before/After**
```java
// Before
  * Added an optional `--node-id` flag to the `FeatureCommand` command. It specifies the node to describe. If not provided, an arbitrary node is used.


// After
---
title: "Documentation Redirect"
robots: "noindex"
_build:
  list: false
---

<!--
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements.  See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
```

**How to spot**
When reviewing docs/documentation code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19977: documentation section on the streams rebalance protocol (#21170).

**Test**
No test added.
