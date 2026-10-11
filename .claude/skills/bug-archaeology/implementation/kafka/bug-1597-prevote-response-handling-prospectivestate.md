**Commit**: 4583b033f0 · **Ticket**: KAFKA-17642
**Subsystem**: checkstyle/suppressions.xml
**Category**: State not cleaned up

**Root cause**
This PR implements the second part of KIP-996 and KAFKA-16164 (tasks KAFKA-16607, KAFKA-17642, KAFKA-17643, KAFKA-17675) which encompass the response handling of PreVotes, addition of new ProspectiveState, update to metrics, and addition of Raft simulation tests. Voters now transition to ProspectiveState first before CandidateState to prevent unnecessary epoch bumps. Voters in ProspectiveState send PreVotes requests which are Vote requests with PreVote set to true. Follower grants PreVotes if it has not yet fetched successfully from leader.

**Before/After**
```java
// Before
              files="(AbstractFetch|Sender|SenderTest|ConsumerCoordinator|KafkaConsumer|KafkaProducer|Utils|TransactionManager|TransactionManagerTest|KafkaAdminClient|NetworkClient|Admin|KafkaRaftClient|KafkaRaftClientTest|RaftClientTestContext|TestingMetricsInterceptingAdminClient).java"/>
              files="(NetworkClient|FieldSpec|KafkaRaftClient|KafkaProducer).java"/>
              files="(KafkaConsumer|ConsumerCoordinator|AbstractFetch|KafkaProducer|AbstractRequest|AbstractResponse|TransactionManager|Admin|KafkaAdminClient|MockAdminClient|KafkaRaftClient|KafkaRaftClientTest|KafkaNetworkChannelTest).java"/>
              files="(AbstractRequest|AbstractResponse|KerberosLogin|WorkerSinkTaskTest|TransactionManagerTest|SenderTest|KafkaAdminClient|ConsumerCoordinatorTest|KafkaAdminClientTest|KafkaRaftClientTest).java"/>
              files="(ConsumerCoordinator|BufferPool|MetricName|Node|ConfigDef|RecordBatch|SslFactory|SslTransportLayer|MetadataResponse|KerberosLogin|Selector|Sender|Serdes|TokenInformation|Agent|PluginUtils|MiniTrogdorCluster|TasksRequest|KafkaProducer|AbstractStickyAssignor|KafkaRaftClient|Authorizer|FetchSessionHandler|RecordAccumulator|Shell).java"/>

// After
              files="(AbstractFetch|Sender|SenderTest|ConsumerCoordinator|KafkaConsumer|KafkaProducer|Utils|TransactionManager|TransactionManagerTest|KafkaAdminClient|NetworkClient|Admin|RaftClientTestContext|TestingMetricsInterceptingAdminClient).java"/>
              files="(NetworkClient|FieldSpec|KafkaProducer).java"/>
              files="(KafkaConsumer|ConsumerCoordinator|AbstractFetch|KafkaProducer|AbstractRequest|AbstractResponse|TransactionManager|Admin|KafkaAdminClient|MockAdminClient|KafkaNetworkChannelTest).java"/>
              files="(AbstractRequest|AbstractResponse|KerberosLogin|WorkerSinkTaskTest|TransactionManagerTest|SenderTest|KafkaAdminClient|ConsumerCoordinatorTest|KafkaAdminClientTest).java"/>
              files="(ConsumerCoordinator|BufferPool|MetricName|Node|ConfigDef|RecordBatch|SslFactory|SslTransportLayer|MetadataResponse|KerberosLogin|Selector|Sender|Serdes|TokenInformation|Agent|PluginUtils|MiniTrogdorCluster|TasksRequest|KafkaProducer|AbstractStickyAssignor|Authorizer|FetchSessionHandler|RecordAccumul
```

**How to spot**
When reviewing checkstyle/suppressions.xml code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17642: prevote response handling and prospectivestate (#18240).

**Test**
Test modified in RequestQuotaTest.scala covering the corrected behaviour.
