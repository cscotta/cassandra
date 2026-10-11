**Commit**: 02c794dfd3 · **Ticket**: KAFKA-15776
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
KIP-1018, part1, Introduce remote.fetch.max.timeout.ms to configure DelayedRemoteFetch timeout

Reviewers: Luke Chen <showuon@gmail.com>

**Before/After**
```java
// Before
    private static final String FETCH_MAX_WAIT_MS_DOC = "The maximum amount of time the server will block before answering the fetch request if there isn't sufficient data to immediately satisfy the requirement given by fetch.min.bytes.";
  extends DelayedOperation(fetchParams.maxWaitMs) {
    val remoteFetch = new DelayedRemoteFetch(remoteFetchTask, remoteFetchResult, remoteFetchInfo,
    val delayedRemoteFetch = new DelayedRemoteFetch(null, future, fetchInfo, Seq(topicIdPartition -> fetchStatus), fetchParams,
      Seq(topicIdPartition -> logReadInfo), replicaManager, callback)
    assertThrows(classOf[IllegalStateException], () => new DelayedRemoteFetch(null, future, fetchInfo, Seq(topicIdPartition -> fetchStatus), fetchParams,
      Seq(topicIdPartition -> logReadInfo), replicaManager, callback))

// After
    private static final String FETCH_MAX_WAIT_MS_DOC = "The maximum amount of time the server will block before " +
            "answering the fetch request there isn't sufficient data to immediately satisfy the requirement given by " +
            "fetch.min.bytes. This config is used only for local log fetch. To tune the remote fetch maximum wait " +
            "time, please refer to 'remote.fetch.max.wait.ms' broker config";
                         remoteFetchMaxWaitMs: Long,
  extends DelayedOperation(remoteFetchMaxWaitMs) {
    val remoteFetchMaxWaitMs = config.remoteLogManagerConfig.remoteFetchMaxWaitMs()
    val remoteFetch = new DelayedRemoteFetch(remoteFetchTask, remoteFetchResult, remoteFetchInfo, remoteFetchMaxWaitMs,
  private val remoteFetchMaxWaitMs = 500
    val delayedRemoteFetch = new DelayedRemoteFetch(null, future, fetchInfo, remoteFetchMaxWaitMs,
      Seq(topicIdPartition -> fetchStatus), fetchParams, Seq(topicIdPartition -> logReadInfo), replicaManager, callback)
    assertThrows(classOf[IllegalStateException], () => new DelayedRemoteFetch(null, future, fetchInfo, remoteFetchMaxWaitMs,
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15776: introduce remote.fetch.max.timeout.ms to configure delayedremotefetch timeout (#14778).

**Test**
Test modified in DelayedRemoteFetchTest.scala covering the corrected behaviour.
