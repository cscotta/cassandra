**Commit**: 5f92f60bff · **Ticket**: KAFKA-17329
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Adds the DefaultStatePersister and other supporting classes for managing share state. * Added DefaultStatePersister implementation. This is the entry point for callers who wish to invoke the share state RPC API. * Added PersisterStateManager which is used by DefaultStatePersister to manage and send the RPCs over the network.

**Before/After**
```java
// Before
import kafka.server.metadata.{AclPublisher, BrokerMetadataPublisher, ClientQuotaMetadataManager, DelegationTokenPublisher, DynamicClientQuotaPublisher, DynamicConfigPublisher, KRaftMetadataCache, ScramPublisher}
import org.apache.kafka.server.share.persister.{NoOpShareStatePersister, Persister}
  var shareCoordinator: Option[ShareCoordinator] = _
      persister = NoOpShareStatePersister.getInstance()

        new Metrics()

// After
    testImplementation project(':clients').sourceSets.test.output
    testImplementation project(':server-common').sourceSets.test.output

    testImplementation libs.mockitoCore
  <allow pkg="org.mockito" />

      <subpackage name="persister">
        <allow pkg="org.apache.kafka.clients" />
        <allow pkg="org.apache.kafka.common.internals" />
        <allow pkg="org.apache.kafka.server.util" />
        <allow pkg="org.apache.kafka.test" />
      </subpackage>
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17329: defaultstatepersister implementation (#17270).

**Test**
Test modified in SharePartitionManagerTest.java covering the corrected behaviour.
