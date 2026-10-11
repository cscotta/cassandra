**Commit**: 4de6a90801 · **Ticket**: KAFKA-16921
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        includeTags 'org.apache.kafka.test.IntegrationTest'
        excludeTags 'org.apache.kafka.test.IntegrationTest'
    testImplementation libs.junitVintageEngine
    testImplementation libs.mockitoJunitJupiter
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
    @Before
    @After
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with

// After
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
    @BeforeEach
    @AfterEach
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16921 [6/n] remove junit 4 dependency from connect:runtime module (#16383).

**Test**
Test modified in TransformationIntegrationTest.java covering the corrected behaviour.
