**Commit**: 4e846038a6 · **Ticket**: KAFKA-10787
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
                               ':metadata',
                               ':server',
                               ':storage',
                               ':storage:storage-api', //  rename in settings.gradle
import java.util.OptionalLong;
import static org.apache.kafka.controller.BrokerControlState.FENCED;
import org.apache.kafka.server.common.TopicIdPartition;
import java.util.Map.Entry;

import org.apache.kafka.metadata.VersionRange;


// After


import java.util.OptionalLong;
import static org.apache.kafka.controller.BrokerControlState.FENCED;
import org.apache.kafka.server.common.TopicIdPartition;


import org.apache.kafka.metadata.VersionRange;

import java.util.Map.Entry;
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-10787 apply spotless to `metadata` and `server` and `storage`  module (#16297).

**Test**
Test modified in AclControlManagerTest.java covering the corrected behaviour.
