**Commit**: 486b991f22 · **Ticket**: KAFKA-18711
**Subsystem**: checkstyle/import-control-server-common.xml
**Category**: Logic error in condition

**Root cause**
Basically, one of the refactor tasks. In this PR, I have moved
`DelegationTokenPublisher` to the metadata module. Similar to the
`ScramPublisher` migration (commit feee50f476), I have moved
`DelegationTokenManager` to the server-common module, as it would
otherwise create a circular dependency. Moreover, I have made multiple
changes throughout the codebase to reference `DelegationTokenManager`
from server-common instead of the server module.

**Before/After**
```java
// Before
import org.apache.kafka.server.DelegationTokenManager;
import org.apache.kafka.metadata.publisher.{AclPublisher, ScramPublisher}
import org.apache.kafka.security.CredentialProvider
import org.apache.kafka.server.{AssignmentsManager, BrokerFeatures, ClientMetricsManager, DefaultApiVersionManager, DelayedActionQueue, DelegationTokenManager, ProcessRole}
          config,
import org.apache.kafka.server.{ApiVersionManager, DelegationTokenManager, ProcessRole}

// After
    <allow pkg="javax.crypto" />
    <!-- allow config classes for server package -->
    <allow pkg="org.apache.kafka.server.config" />

import org.apache.kafka.security.DelegationTokenManager;
import org.apache.kafka.metadata.publisher.{AclPublisher, DelegationTokenPublisher, ScramPublisher}
import org.apache.kafka.security.{CredentialProvider, DelegationTokenManager}
import org.apache.kafka.server.{AssignmentsManager, BrokerFeatures, ClientMetricsManager, DefaultApiVersionManager, DelayedActionQueue, ProcessRole}
          config.nodeId,
import org.apache.kafka.security.DelegationTokenManager
import org.apache.kafka.server.{ApiVersionManager, ProcessRole}
```

**How to spot**
When reviewing checkstyle/import-control-server-common.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18711 move delegationtokenpublisher to metadata module (#20475).

**Test**
Test modified in BrokerMetadataPublisherTest.scala covering the corrected behaviour.
