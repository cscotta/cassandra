**Commit**: 2694d7aad9 · **Ticket**: KAFKA-19248
**Subsystem**: connect/runtime
**Category**: Logic error in condition

**Root cause**
This adds tests for [KIP-891](https://cwiki.apache.org/confluence/display/KAFKA/KIP-891%3A+Running+multiple+versions+of+Connector+plugins). It primarily focuses on tests for the new additions in plugin loading
isolation. It has dependency on the actual KIP implementation PRs and
should be merged post https://github.com/apache/kafka/pull/17742

Reviewers: Greg Harris <greg.harris@aiven.io>

**Before/After**
```java
// Before
    public TransformationPluginRecommender transformationPluginRecommender(String classOrAlias) {
        return new TransformationPluginRecommender(classOrAlias);
    public PredicatePluginRecommender predicatePluginRecommender(String classOrAlias) {
        return new PredicatePluginRecommender(classOrAlias);

// After
    public TransformationPluginRecommender transformationPluginRecommender(String classOrAliasConfig) {
        return new TransformationPluginRecommender(classOrAliasConfig);
    public PredicatePluginRecommender predicatePluginRecommender(String classOrAliasConfig) {
        return new PredicatePluginRecommender(classOrAliasConfig);
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
```

**How to spot**
When reviewing connect/runtime code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19248: multiversioning in kafka connect - plugin loading isolation tests (#18325).

**Test**
Test modified in MultiVersionTest.java covering the corrected behaviour.
