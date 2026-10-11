**Commit**: 93e65c4539 · **Ticket**: KAFKA-18267
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
There is some redundant code that could be removed in `CloseOptions`. This patch also adds unit tests for CloseOptions. Reviewers: Ken Huang <s7133700@gmail.com>, PoAn Yang
<payang@apache.org>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    protected GroupMembershipOperation operation = GroupMembershipOperation.DEFAULT;
    protected Optional<Duration> timeout = Optional.empty();
    protected CloseOptions(final CloseOptions option) {
        this.operation = option.operation;
        this.timeout = option.timeout;
    }

        CloseOptions option = new CloseOptions();
        option.timeout = Optional.ofNullable(timeout);
        return option;
        Objects.requireNonNull(operation, "operation should not be null");
        CloseOptions option = new CloseOptions();

// After
    private GroupMembershipOperation operation = GroupMembershipOperation.DEFAULT;
    private Optional<Duration> timeout = Optional.empty();
        return new CloseOptions().withTimeout(timeout);
        return new CloseOptions().withGroupMembershipOperation(operation);
        this.operation = Objects.requireNonNull(operation, "operation should not be null");
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18267 add unit tests for closeoptions (#19571).

**Test**
Test modified in CloseOptionsTest.java covering the corrected behaviour.
