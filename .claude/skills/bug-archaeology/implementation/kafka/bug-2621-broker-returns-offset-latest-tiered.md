**Commit**: 84add30ea5 · **Ticket**: KAFKA-16154
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
This pr support EarliestLocalSpec LatestTierSpec in GetOffsetShell, and add integration tests. Reviewers: Luke Chen <showuon@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>, PoAn Yang <payang@apache.org>

**Before/After**
```java
// Before
        } else if (offsetSpec instanceof OffsetSpec.LatestTierSpec) {
    public static class LatestTierSpec extends OffsetSpec { }
     * Used to retrieve the offset with the local log start offset,
     * log start offset is the offset of a log above which reads are guaranteed to be served
     * from the disk of the leader broker, when Tiered Storage is not enabled, it behaves the same
     * as the earliest timestamp
    public static OffsetSpec earliestLocalSpec() {
     * Used to retrieve the offset with the highest offset of data stored in remote storage,
     * and when Tiered Storage is not enabled, we won't return any offset (i.e. Unknown offset)
    public static OffsetSpec latestTierSpec() {
        return new LatestTierSpec();
            env.adminClient().listOffsets(Collections.singletonMap(tp0, OffsetSpec.earliestLocalSpec()));

// After
    testImplementation project(':storage').sourceSets.test.output
    <allow pkg="org.apache.kafka.server.log.remote.metadata.storage" />
    <allow pkg="org.apache.kafka.server.log.remote.storage" />
        } else if (offsetSpec instanceof OffsetSpec.LatestTieredSpec) {
    public static class LatestTieredSpec extends OffsetSpec { }
     * Used to retrieve the local log start offset.
     * Local log start offset is the offset of a log above which reads
     * are guaranteed to be served from the disk of the leader broker.
     * <br/>
     * Note: When tiered Storage is not enabled, it behaves the same as retrieving the earliest timestamp offset.
    public static OffsetSpec earliestLocal() {
     * Used to retrieve the highest offset of data stored in remote storage.
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16154: broker returns offset for latest_tiered_timestamp (#16783).

**Test**
Test modified in KafkaAdminClientTest.java covering the corrected behaviour.
