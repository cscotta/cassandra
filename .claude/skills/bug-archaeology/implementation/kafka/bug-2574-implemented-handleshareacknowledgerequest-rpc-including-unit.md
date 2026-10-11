**Commit**: 5b47975bb9 · **Ticket**: KAFKA-16746
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
Implemented handleShareAcknowledge request RPC in KafkaApis.scala. This method is called whenever the client sends a Share Acknowledge request to the broker. The acknowledge logic is handles asynchronously and the results are handled appropriately. Reviewers:  Apoorv Mittal <apoorvmittal10@gmail.com>, Jun Rao <junrao@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.common.requests.ShareFetchMetadata;
    private ShareFetchMetadata nextMetadata;
        this.nextMetadata = ShareFetchMetadata.initialEpoch(memberId);
        public static ShareAcknowledgeRequest.Builder forConsumer(String groupId, ShareFetchMetadata metadata,
        public static Builder forConsumer(String groupId, ShareFetchMetadata metadata,
public class ShareFetchMetadata {
    public ShareFetchMetadata(Uuid memberId, int epoch) {
    public static ShareFetchMetadata initialEpoch(Uuid memberId) {
        return new ShareFetchMetadata(memberId, INITIAL_EPOCH);
    public ShareFetchMetadata nextEpoch() {
        return new ShareFetch

// After
import org.apache.kafka.common.requests.ShareRequestMetadata;
    private ShareRequestMetadata nextMetadata;
        this.nextMetadata = ShareRequestMetadata.initialEpoch(memberId);
        public static ShareAcknowledgeRequest.Builder forConsumer(String groupId, ShareRequestMetadata metadata,
        public static Builder forConsumer(String groupId, ShareRequestMetadata metadata,
public class ShareRequestMetadata {
    public ShareRequestMetadata(Uuid memberId, int epoch) {
    public static ShareRequestMetadata initialEpoch(Uuid memberId) {
        return new ShareRequestMetadata(memberId, INITIAL_EPOCH);
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16746: implemented handleshareacknowledgerequest rpc including unit tests (#16792).

**Test**
Test modified in ShareSessionHandlerTest.java covering the corrected behaviour.
