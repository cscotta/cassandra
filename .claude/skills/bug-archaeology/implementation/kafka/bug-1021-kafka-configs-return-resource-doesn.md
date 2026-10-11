**Commit**: d1f1e5c8fd · **Ticket**: KAFKA-18904
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
* Return resource doesn't exist message when users try to describe a
non-existent resource in kafka-configs.sh and kafka-client-metrics.sh. * For groups type, the command checks both existent groups and
non-existent groups but having dynamic config. If it cannot find a group
in both conditions, return resource doesn't exist message. Reviewers: Lan Ding <53332773+DL1231@users.noreply.github.com>, Andrew
Schofield <aschofield@confluent.io>

---------

Signed-off-by: PoAn Yang <payang@apache.org>

**Before/After**
```java
// Before
        throw new UnsupportedOperationException("Not implemented yet");
 *    http://www.apache.org/licenses/LICENSE-2.0
import org.apache.kafka.clients.admin.{Admin, AlterClientQuotasOptions, AlterConfigOp, AlterConfigsOptions, ConfigEntry, DescribeClusterOptions, DescribeConfigsOptions, ListTopicsOptions, ScramCredentialInfo, UserScramCredentialDeletion, UserScramCredentialUpsertion, ScramMechanism => PublicScramMechanism}

// After
import java.util.Set;
    public static ListConfigResourcesResult listConfigResourcesResult(Map<ConfigResource.Type, Set<String>> resourceNames) {
        Collection<ConfigResource> resources = resourceNames.entrySet().stream()
            .flatMap(entry -> entry.getValue().stream()
                .map(name -> new ConfigResource(entry.getKey(), name)))
            .collect(Collectors.toList());
        return new ListConfigResourcesResult(KafkaFuture.completedFuture(resources));
    }

import java.util.HashSet;
        KafkaFutureImpl<Collection<ConfigResource>> future = new KafkaFutureImpl<>();
        Set<ConfigResource> configResources = new HashSet<>();
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18904: kafka-configs.sh return resource doesn't exist message [3/n] (#19808).

**Test**
Test modified in AdminClientTestUtils.java covering the corrected behaviour.
