**Commit**: 0dc74c5556 · **Ticket**: KAFKA-17232
**Subsystem**: connect/mirror
**Category**: Logic error in condition

**Root cause**
Reviewers: Hongten <hongtenzone@foxmail.com>, Chris Egerton <chrise@aiven.io>

**Before/After**
```java
// Before
    private Set<String> knownConsumerGroups = Collections.emptySet();
        String connectorName = config.connectorName();
        log.info("Started {} with {} consumer groups.", connectorName, knownConsumerGroups.size());
        log.debug("Started {} with consumer groups: {}", connectorName, knownConsumerGroups);
            knownConsumerGroups = consumerGroups;

// After
import org.apache.kafka.connect.errors.RetriableException;
    private Set<String> knownConsumerGroups = null;
        if (knownConsumerGroups == null) {
            // If knownConsumerGroup is null, it means the initial loading has not finished.
            // An exception should be thrown to trigger the retry behavior in the framework.
            log.debug("Initial consumer loading has not yet completed");
            throw new RetriableException("Timeout while loading consumer groups.");
        }

        // If loadInitialConsumerGroups fails for any reason(e.g., timeout), knownConsumerGroups may be null.
        // We still want this method to recover gracefully in such cases.
        Set<String> knownConsumerGroups = this.knownConsumerGroups == null ? Collections.emptySet() : this.knownConsumerGroups;
```

**How to spot**
When reviewing connect/mirror code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17232: do not generate task configs in mirrorcheckpointconnector if initial consumer group load times out (#16767).

**Test**
Test modified in MirrorCheckpointConnectorTest.java covering the corrected behaviour.
