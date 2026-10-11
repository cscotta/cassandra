**Commit**: 6a44fb154d · **Ticket**: KAFKA-16523
**Subsystem**: build.gradle
**Category**: Wrong serialization / deserialization

**Root cause**
This PR adds support for add-controller and remove-controller in the kafka-metadata-quorum.sh
command-line tool. It also fixes some minor server-side bugs that blocked the tool from working. In kafka-metadata-quorum.sh, the implementation of remove-controller is fairly straightforward. It
just takes some command-line flags and uses them to invoke AdminClient.

**Before/After**
```java
// Before
// (no removals)

// After
    implementation project(':metadata')
    implementation project(':server')
    <allow pkg="org.apache.kafka.metadata.properties" />
    <allow pkg="org.apache.kafka.network" />
import java.util.Optional;

    private Optional<String> clusterId = Optional.empty();

    public AddRaftVoterOptions setClusterId(Optional<String> clusterId) {
        this.clusterId = clusterId;
        return this;
    }
```

**How to spot**
When reviewing build.gradle code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16523; kafka-metadata-quorum: support add-controller and remove-controller (#16774).

**Test**
Test modified in KafkaAdminClientTest.java covering the corrected behaviour.
