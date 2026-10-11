**Commit**: f5dd661cb5 · **Ticket**: KAFKA-18396
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
After log4j migration, we need to update the logging configuration in KafkaDockerWrapper from log4j1 to log4j2. Reviewers: Manikumar Reddy <manikumar.reddy@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
    implementation libs.jacksonDatabindYaml
  <subpackage name="docker">
    <allow class="com.fasterxml.jackson.annotation.JsonAnyGetter" />
    <allow class="com.fasterxml.jackson.annotation.JsonAnySetter" />
    <allow class="com.fasterxml.jackson.annotation.JsonProperty" />
    <allow class="com.fasterxml.jackson.annotation.JsonPropertyOrder" />
    <allow class="com.fasterxml.jackson.annotation.JsonIgnoreProperties" />
  </subpackage>

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18396: migrate log4j1 configuration to log4j2 in kafkadockerwrapper (#18394).

**Test**
Test modified in KafkaDockerWrapperTest.scala covering the corrected behaviour.
