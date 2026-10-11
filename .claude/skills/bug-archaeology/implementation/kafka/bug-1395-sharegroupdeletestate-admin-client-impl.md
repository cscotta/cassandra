**Commit**: 3fc103b48b · **Ticket**: KAFKA-18629
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
* In this PR, we add various infra classes needed to support the
`deleteShareGroups` functionality via the `kafka-share-groups.sh`
script, as well as the implementation of `kafka-share-groups.sh --delete`. Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
import org.apache.kafka.common.annotation.InterfaceStability;
import java.util.HashMap;
 * The result of the {@link Admin#deleteConsumerGroups(Collection)} call.
 *
 * The API of this class is evolving, see {@link Admin} for details.
@InterfaceStability.Evolving
public class DeleteConsumerGroupsResult {
    private final Map<String, KafkaFuture<Void>> futures;

    DeleteConsumerGroupsResult(final Map<String, KafkaFuture<Void>> futures) {
        this.futures = futures;
    }

// After
    /**
     * Delete share groups from the cluster with the default options.
     *
     * @return The DeleteShareGroupsResult.
     */
    default DeleteShareGroupsResult deleteShareGroups(Collection<String> groupIds) {
        return deleteShareGroups(groupIds, new DeleteShareGroupsOptions());
    }

    /**
     * Delete share groups from the cluster.
     *
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18629: sharegroupdeletestate admin client impl. (#18928).

**Test**
Test modified in MockAdminClient.java covering the corrected behaviour.
