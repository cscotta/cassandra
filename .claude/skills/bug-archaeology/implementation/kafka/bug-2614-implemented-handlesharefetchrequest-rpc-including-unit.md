**Commit**: 1db84c1a11 · **Ticket**: KAFKA-16745
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
Implemented handleShareFetch request RPC in KafkaApis.scala. This method is called whenever the client sends a Share Fetch request to the broker. Although Share Fetch request support acknowledgements, since the logic for acknowledging records is not completely implemented in SharePartitionManager.java class, this method currently includes placeholder code for acknowledging, which will be replaced by the actual functionality in the upcoming PRs. Reviewers:  Apoorv Mittal <apoorvmittal10@gmail.com>, Abhinav Dixit <adixit@confluent.io>, Jun Rao <junrao@gmail.com>

**Before/After**
```java
// Before
    int responseSize(LinkedHashMap<TopicIdPartition, ShareFetchResponseData.PartitionData> updates, short version) {
    ShareFetchResponse updateAndGenerateResponseData(String groupId, Uuid memberId,
    ErroneousAndValidPartitionData getErroneousAndValidTopicIdPartitions() {
    ShareFetchResponse throttleResponse(int throttleTimeMs) {
    abstract int responseSize(LinkedHashMap<TopicIdPartition, ShareFetchResponseData.PartitionData> updates,
    abstract ShareFetchResponse updateAndGenerateResponseData(String groupId, Uuid memberId, LinkedHashMap<TopicIdPartition, ShareFetchResponseData.PartitionData> updates);

// After
import kafka.server.share.SharePartitionManager;
    private Optional<SharePartitionManager> sharePartitionManager = Optional.empty();
    public KafkaApisBuilder setSharePartitionManager(Optional<SharePartitionManager> sharePartitionManager) {
        this.sharePartitionManager = sharePartitionManager;
        return this;
    }

                             OptionConverters.toScala(sharePartitionManager),
    public int responseSize(LinkedHashMap<TopicIdPartition, ShareFetchResponseData.PartitionData> updates, short version) {
    public ShareFetchResponse updateAndGenerateResponseData(String groupId, Uuid memberId,
    public ErroneousAndValidPartitionData getErroneousAndValidTopicIdPartitions() {
    public ShareFetchResponse throttleResponse(int throttleTimeMs) {
```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16745: implemented handlesharefetchrequest rpc including unit tests (#16456).

**Test**
Test modified in SharePartitionManagerTest.java covering the corrected behaviour.
