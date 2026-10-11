**Commit**: 84bc0c26ee · **Ticket**: KAFKA-18224
**Subsystem**: tools/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
     * set to "true" to prevent missing events as well as repeat consumption.
        cleanUpConfig.put(ConsumerConfig.GROUP_PROTOCOL_CONFIG, GroupProtocol.CLASSIC.name);

// After
import org.apache.kafka.clients.consumer.GroupProtocol;
     * set to "true" to prevent missing events as well as repeat consumption. This also sets
     * {@link ConsumerConfig#GROUP_PROTOCOL_CONFIG} to "classic".
        filtered.setProperty(ConsumerConfig.GROUP_PROTOCOL_CONFIG, GroupProtocol.CLASSIC.name);
import org.apache.kafka.clients.consumer.GroupProtocol;
import static org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_PROTOCOL_CONFIG;

                if (consumerConfig.containsKey(GROUP_PROTOCOL_CONFIG) &&
                    !consumerConfig.get(GROUP_PROTOCOL_CONFIG).toString().equalsIgnoreCase(GroupProtocol.CLASSIC.name())
                ) {
                    System.out.println("WARNING: provided group protocol will be ignored. Using supported " + GroupProtocol.CLASSIC.name() + " protocol instead");
                }
```

**How to spot**
When reviewing tools/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18224: explicit group protocol setting in streams resetter (#18172).

**Test**
Test modified in IntegrationTestUtils.java covering the corrected behaviour.
