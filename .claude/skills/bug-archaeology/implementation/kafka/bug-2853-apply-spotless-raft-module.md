**Commit**: d239dde8f6 · **Ticket**: KAFKA-10787
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
                               ':raft',
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Objects;
import org.apache.kafka.common.Uuid;
import org.apache.kafka.raft.generated.QuorumStateData;
import org.apache.kafka.raft.internals.ReplicaKey;
import org.apache.kafka.raft.internals.ReplicaKey;
import org.apache.kafka.common.utils.Utils;

// After

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import java.util.Objects;

import org.apache.kafka.common.Uuid;
import org.apache.kafka.raft.generated.QuorumStateData;
import org.apache.kafka.raft.internals.ReplicaKey;
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-10787 apply spotless to raft module (#16278).

**Test**
Test modified in CandidateState.java covering the corrected behaviour.
