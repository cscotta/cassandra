**Commit**: a95522a5ba · **Ticket**: KAFKA-19042
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
This PR does the following:
1) Rewrites consumerBounceTest in Java. 2) Moves the test to clients-integration-test. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before

    private final Set<TopicPartition> partitionAssignment = new HashSet<>();
    private final List<String> topicsSubscription;
    public ConsumerAssignmentPoller(
        Consumer<byte[], byte[]> consumer,
        List<String> topicsToSubscribe
    ) {
    public ConsumerAssignmentPoller(
        Consumer<byte[], byte[]> consumer,
        List<String> topicsToSubscribe,
        Set<TopicPartition> partitionsToAssign,
        ConsumerRebalanceListener userRebalanceListener

// After
    testImplementation project(':server-common').sourceSets.test.output
    <allow pkg="org.slf4j"/>
import org.apache.kafka.common.errors.WakeupException;
import java.util.Collections;
import java.util.Optional;
    private final Set<TopicPartition> partitionsToAssign;
    private volatile Optional<Throwable> thrownException = Optional.empty();
    private volatile int receivedMessages = 0;
    private final Set<TopicPartition> partitionAssignment = Collections.synchronizedSet(new HashSet<>());
    private List<String> topicsSubscription;
    public ConsumerAssignmentPoller(Consumer<byte[], byte[]> consumer, List<String> topicsToSubscribe) {
    public ConsumerAssignmentPoller(Consumer<byte[], byte[]> consumer, Set<TopicPartition> partitionsToAssign) {
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19042 rewrite consumerbouncetest by java (#19822).

**Test**
Test modified in import-control-clients-integration-tests.xml covering the corrected behaviour.
