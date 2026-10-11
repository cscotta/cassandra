**Commit**: cb9c6718fa · **Ticket**: KAFKA-18722
**Subsystem**: storage/src
**Category**: Race condition / missing synchronization

**Root cause**
Reviewers: Luke Chen <showuon@gmail.com>, Christo Lolov <lolovc@amazon.com>

**Before/After**
```java
// Before
import java.util.Optional;
    public void startConsumerThread() {
    public void waitTillConsumptionCatchesUp(RecordMetadata recordMetadata) throws TimeoutException {
    public void waitTillConsumptionCatchesUp(RecordMetadata recordMetadata,
                                             long timeoutMs) throws TimeoutException {
    public void addAssignmentsForPartitions(Set<TopicIdPartition> partitions) {
    public void removeAssignmentsForPartitions(Set<TopicIdPartition> partitions) {

    public Optional<Long> readOffsetForPartition(int metadataPartition) {
        return consumerTask.readOffsetForMetadataPartition(metadataPartition);
    }
    // public for testing

// After
    void startConsumerThread() {
    void waitTillConsumptionCatchesUp(RecordMetadata recordMetadata) throws TimeoutException {
    void waitTillConsumptionCatchesUp(RecordMetadata recordMetadata,
                                      long timeoutMs) throws TimeoutException {
    void addAssignmentsForPartitions(Set<TopicIdPartition> partitions) {
    void removeAssignmentsForPartitions(Set<TopicIdPartition> partitions) {
    // visible for testing
    void ingestRecords() {
    // visible for testing
    void closeConsumer() {
    void addAssignmentsForPartitions(final Set<TopicIdPartition> partitions) {
    void removeAssignmentsForPartitions(final Set<TopicIdPartition> partitions) {
```

**How to spot**
When reviewing storage/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18722: remove the unreferenced methods in tbrlmm and consumermanager (#18791).

**Test**
No test added.
