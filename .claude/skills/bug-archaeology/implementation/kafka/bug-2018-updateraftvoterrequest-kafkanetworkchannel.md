**Commit**: a8f84cab95 · **Ticket**: KAFKA-18001
**Subsystem**: checkstyle/suppressions.xml
**Category**: Logic error in condition

**Root cause**
Adds support for UpdateRaftVoterRequest in KafkaNetworkChannel. This addresses the following scenario:

* Bootstrap a KRaft Controller quorum in dynamic mode
* Start additional controllers (as observers)
* Update kraft.version feature from 0 to 1
* Use kafka-metadata-quorum add-controller to promote an observer controller to a follower

Reviewers: Colin Patrick McCabe <cmccabe@apache.org>, Alyssa Huang <ahuang@confluent.io>

**Before/After**
```java
// Before
              files="(KafkaConsumer|ConsumerCoordinator|AbstractFetch|KafkaProducer|AbstractRequest|AbstractResponse|TransactionManager|Admin|KafkaAdminClient|MockAdminClient|KafkaRaftClient|KafkaRaftClientTest).java"/>
        ApiKeys.FETCH_SNAPSHOT

// After
              files="(KafkaConsumer|ConsumerCoordinator|AbstractFetch|KafkaProducer|AbstractRequest|AbstractResponse|TransactionManager|Admin|KafkaAdminClient|MockAdminClient|KafkaRaftClient|KafkaRaftClientTest|KafkaNetworkChannelTest).java"/>
import org.apache.kafka.common.message.UpdateRaftVoterRequestData;
import org.apache.kafka.common.requests.UpdateRaftVoterRequest;
        if (requestData instanceof UpdateRaftVoterRequestData)
            return new UpdateRaftVoterRequest.Builder((UpdateRaftVoterRequestData) requestData);
import org.apache.kafka.common.feature.SupportedVersionRange;
import org.apache.kafka.common.message.UpdateRaftVoterResponseData;
import org.apache.kafka.common.requests.UpdateRaftVoterResponse;
        ApiKeys.FETCH_SNAPSHOT,
        ApiKeys.UPDATE_RAFT_VOTER
            case UPDATE_RAFT_VOTER:
                return RaftUtil.updateVoterRequest(
```

**How to spot**
When reviewing checkstyle/suppressions.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18001: support updateraftvoterrequest in kafkanetworkchannel (#17773).

**Test**
Test modified in KafkaNetworkChannelTest.java covering the corrected behaviour.
