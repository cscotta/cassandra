**Commit**: 43fdc6ae08 · **Ticket**: KAFKA-17122
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.common.Uuid;
        private Uuid clusterId;
        public Builder setClusterId(Uuid clusterId) {
                    setClusterId(clusterId.toString()).
                        .setClusterId(Uuid.randomUuid())
                        .setClusterId(Uuid.randomUuid())
import org.apache.kafka.common.Uuid;
        private Uuid clusterId;
        public Builder setClusterId(Uuid clusterId) {
        private Uuid clusterId;
        public Builder setClusterId(Uuid clusterId) {
                clusterId = Uuid.randomUuid();

// After
        private String clusterId;
        public Builder setClusterId(String clusterId) {
                    setClusterId(clusterId).
                        .setClusterId(Uuid.randomUuid().toString())
                        .setClusterId(Uuid.randomUuid().toString())
        private String clusterId;
        public Builder setClusterId(String clusterId) {
        private String clusterId;
        public Builder setClusterId(String clusterId) {
                clusterId = Uuid.randomUuid().toString();
    private final String clusterId;
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17122 change the type of `clusterid` from `uuid` to `string` (#16590).

**Test**
Test modified in BrokerNode.java covering the corrected behaviour.
