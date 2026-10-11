**Commit**: d82f03e98b · **Ticket**: KAFKA-18364
**Subsystem**: docs/upgrade.html
**Category**: Logic error in condition

**Root cause**
This patch introduces a new page to document the configs and metrics that have been removed in the transition to 4.0. While these removed items lack a formal deprecation cycle as they are part of KIP-500, KIP-500 itself does not provide an exhaustive list of all impacted configs and metrics. Therefore, this new page aims to assist Kafka users in understanding the specific configs and metrics that have been removed in the 4.0 release. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
        <li>
            Before updating your Kafka cluster which build on Zookeeper to Kafka 4.0, you must understand what the updates and changes
            are between Zookeeper mode and Kraft mode. See the following page to learn about some of the significant changes in
            latest Kafka releases.
            <!--#include virtual="zk2kraft.html" -->
        </li>
<!--
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements.  See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
```

**How to spot**
When reviewing docs/upgrade.html code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18364 add document to show the changes of metrics and configs after removing zookeeper (#18365).

**Test**
No test added.
