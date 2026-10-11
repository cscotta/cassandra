**Commit**: a797d644bc · **Ticket**: KAFKA-19244
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
This PR implements all the options for `--reset-offset`, (supports
`--execute`, `--dry-run`, `--export`). Includes unit and integration
tests. - --reset-offsets --input-topic String: topics --shift-by Long
- --reset-offsets --input-topic String: topics --to-offset  Long
- --reset-offsets --input-topic String: topics -to-latest
- --reset-offsets --input-topic String: topics -to-earliest
- --reset-offsets --all-input-topics --shift-by Long
- --reset-offsets --all-input-topics --to-offset  Long
- --reset-offsets --all-input-topics -to-latest
- --reset-offsets --all-input-topics -to-earliest
- --reset-offsets --all-input-topics -to-earliest
- --reset-offsets --from-file String:fileName
- --reset...

**Before/After**
```java
// Before
    ListStreamsGroupOffsetsSpec topicPartitions(Collection<TopicPartition> topicPartitions) {
    Collection<TopicPartition> topicPartitions() {

// After
    public ListStreamsGroupOffsetsSpec topicPartitions(Collection<TopicPartition> topicPartitions) {
    public Collection<TopicPartition> topicPartitions() {
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19244: add support for kafka-streams-groups.sh options (reset-offsets) [1/n] (#19646).

**Test**
Test modified in DeleteStreamsGroupOffsetTest.java covering the corrected behaviour.
