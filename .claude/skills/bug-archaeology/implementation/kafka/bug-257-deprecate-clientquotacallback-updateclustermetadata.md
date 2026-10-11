**Commit**: 86328c25ee · **Ticket**: KAFKA-19566
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
This method was unsupported in KRaft mode until Kafka 4.0, but the
implementation has known issues: memory pressure from immutable Cluster
objects on every update, confusing/irrelevant fields in KRaft context,
and inconsistent partition info due to listener parsing differences. KIP-1162 proposed a redesign, but given the method went years without
user complaints, we're deprecating it now and removing it in Kafka 5.0. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>, Ken Huang
 <s7133700@gmail.com>

**Before/After**
```java
// Before
     * This callback is invoked whenever there are changes in the cluster metadata, such as
    boolean updateClusterMetadata(Cluster cluster);
import org.apache.kafka.common.Cluster
      override def updateClusterMetadata(cluster: Cluster): Boolean = false

// After
        @SuppressWarnings("removal")
     * This callback is invoked whenever there are changes in the cluster metadata, such as
     * @deprecated since 4.4 and should not be used any longer.
    @Deprecated(since = "4.4", forRemoval = true)
    default boolean updateClusterMetadata(Cluster cluster) {
        return false;
    }
  @SuppressWarnings(Array("removal"))
## Upgrading to 4.4.0

### Upgrading Servers to 4.4.0 from any version 3.3.x through 4.3.0

```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19566 deprecate clientquotacallback#updateclustermetadata (#21958).

**Test**
Test modified in CustomQuotaCallbackTest.java covering the corrected behaviour.
