**Commit**: 23e2af1e0f · **Ticket**: KAFKA-20192
**Subsystem**: server/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Mickael Maison <mickael.maison@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.test.TestUtils;
    static void checkKRaftVersions(Admin admin, short finalized) throws Exception {
                TestUtils.retryOnExceptionWithTimeout(30_000, () -> {
                    checkKRaftVersions(admin, KRaftVersion.KRAFT_VERSION_0.featureLevel());
                });
                TestUtils.retryOnExceptionWithTimeout(30_000, () -> {
                    checkKRaftVersions(admin, KRaftVersion.KRAFT_VERSION_1.featureLevel());
                });
    static Map<Integer, Uuid> findVoterDirs(Admin admin) throws Exception {
        quorumInfo.voters().forEach(v -> {
            result.put(v.replicaId(), v.replicaDirectoryId());
        });

// After
import static org.apache.kafka.test.TestUtils.assertFutureThrows;
import static org.apache.kafka.test.TestUtils.retryOnExceptionWithTimeout;
    private static void checkKRaftVersions(Admin admin, short finalized) throws Exception {
                retryOnExceptionWithTimeout(30_000, () ->
                    checkKRaftVersions(admin, KRaftVersion.KRAFT_VERSION_0.featureLevel()));
                retryOnExceptionWithTimeout(30_000, () ->
                    checkKRaftVersions(admin, KRaftVersion.KRAFT_VERSION_1.featureLevel()));
    private static Map<Integer, Uuid> findVoterDirs(Admin admin) throws Exception {
        quorumInfo.voters().forEach(v -> result.put(v.replicaId(), v.replicaDirectoryId()));
                retryOnExceptionWithTimeout(30_000, 10, () -> {
                retryOnExceptionWithTimeout(30_000, 10, () -> {
                int port = port(admin, 3000);
```

**How to spot**
When reviewing server/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20192: fix brittle logic in reconfigurablequorumintegrationtest (#21519).

**Test**
Test modified in ReconfigurableQuorumIntegrationTest.java covering the corrected behaviour.
