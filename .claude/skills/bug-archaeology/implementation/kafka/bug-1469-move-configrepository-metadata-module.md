**Commit**: 21645ebf0b · **Ticket**: KAFKA-18705
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Reviewers: TengYao Chi <kitingiao@gmail.com>, Christo Lolov <lolovc@amazon.com>

**Before/After**
```java
// Before
import kafka.server.metadata.ConfigRepository;
import kafka.server.metadata.ConfigRepository;
import kafka.server.metadata.ConfigRepository
import kafka.server.metadata.ConfigRepository
import kafka.server.metadata.ConfigRepository

// After
    implementation project(':metadata').sourceSets.test.output
import org.apache.kafka.metadata.ConfigRepository;
import org.apache.kafka.metadata.ConfigRepository;
import org.apache.kafka.metadata.ConfigRepository
import org.apache.kafka.metadata.ConfigRepository
import org.apache.kafka.metadata.ConfigRepository
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18705: move configrepository to metadata module (#18784).

**Test**
Test modified in MockConfigRepository.scala covering the corrected behaviour.
