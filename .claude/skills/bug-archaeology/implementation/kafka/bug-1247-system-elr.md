**Commit**: e3430fef88 · **Ticket**: KAFKA-19072
**Subsystem**: tests/kafkatest
**Category**: Logic error in condition

**Root cause**
Verified locally. ```
test_id:    kafkatest.tests.core.eligible_leader_replicas_test.EligibleLeaderReplicasTest.test_basic_eligible_leader_replicas.metadata_quorum=ISOLATED_KRAFT.group_protocol=classic
status:     PASS
run time:   2 minutes 6.074 seconds
--------------------------------------------------------------------------------
test_id:    kafkatest.tests.core.eligible_leader_replicas_test.EligibleLeaderReplicasTest.test_basic_eligible_leader_replicas.metadata_quorum=ISOLATED_KRAFT.group_protocol=consumer
status:     PASS
run time:   2 minutes 6.611 seconds
--------------------------------------------------------------------------------
```

Reviewers: Justine Olshan <jolshan@confluent...

**Before/After**
```java
// Before
// (no removals)

// After
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
```

**How to spot**
When reviewing tests/kafkatest code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19072: add system test for elr (#19344).

**Test**
Test modified in eligible_leader_replicas_test.py covering the corrected behaviour.
