**Commit**: 2637d12e2f · **Ticket**: KAFKA-16908
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    new KafkaNetworkChannel(time, listenerName, netClient, config.quorumRequestTimeoutMs, threadNamePrefix)
      config.quorumRequestTimeoutMs,
  /** ********* Raft Quorum Configuration *********/
  val quorumVoters = getList(QuorumConfig.QUORUM_VOTERS_CONFIG)
  val quorumBootstrapServers = getList(QuorumConfig.QUORUM_BOOTSTRAP_SERVERS_CONFIG)
  val quorumElectionTimeoutMs = getInt(QuorumConfig.QUORUM_ELECTION_TIMEOUT_MS_CONFIG)
  val quorumFetchTimeoutMs = getInt(QuorumConfig.QUORUM_FETCH_TIMEOUT_MS_CONFIG)
  val quorumElectionBackoffMs = getInt(QuorumConfig.QUORUM_ELECTION_BACKOFF_MAX_MS_CONFIG)
  val quorumLingerMs = getInt(QuorumConfig.QUORUM_LINGER_MS_CONFIG)
  val quorumRequestTimeoutMs = getInt(QuorumConfig.QUORUM_REQUEST_TIMEOUT_MS_CONFIG)
  val quorumRetryBackoffMs = getInt(QuorumConfig.QUORUM_RETRY_BACKOFF_MS_CONFIG)


// After
    new KafkaNetworkChannel(time, listenerName, netClient, config.quorumConfig.requestTimeoutMs, threadNamePrefix)
      config.quorumConfig.requestTimeoutMs,
  private val _quorumConfig = new QuorumConfig(this)
  def quorumConfig: QuorumConfig = _quorumConfig

    val voterIds = QuorumConfig.parseVoterIds(quorumConfig.voters)
      if (voterIds.isEmpty && quorumConfig.bootstrapServers.isEmpty) {
      if (voterIds.isEmpty && quorumConfig.bootstrapServers.isEmpty) {
    CompletableFuture.completedFuture(QuorumConfig.parseVoterConnections(config.quorumConfig.voters)),
    QuorumConfig.parseBootstrapServers(config.quorumConfig.bootstrapServers),
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16908 refactor `quorumconfig` with `abstractconfig` (#17231).

**Test**
Test modified in TestRaftServer.scala covering the corrected behaviour.
