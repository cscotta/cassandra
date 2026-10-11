**Commit**: 28ff2a47e6 · **Ticket**: KAFKA-19426
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
refer to https://github.com/apache/kafka/pull/20008. I submit another
proposal to solve the issue which can also help to solve the following
issue:   the
DEFAULT_REMOTE_LOG_METADATA_INITIALIZATION_RETRY_MAX_TIMEOUT_MS is hard
to be defined due to that it relay on the startup process. If it the value is not well defined. the initial will fail and cause
local disk never get deleted.

**Before/After**
```java
// Before
public class TopicBasedRemoteLogMetadataManager implements RemoteLogMetadataManager {

// After
import org.apache.kafka.server.log.remote.metadata.storage.BrokerReadyCallback
      remoteLogManagerOpt.foreach(rlm =>
        rlm.remoteLogMetadataManager() match {
          case callback: BrokerReadyCallback =>
            try {
              callback.onBrokerReady()
            } catch {
              case e: Exception =>
                error(s"Error executing broker ready callback: ${callback.getClass.getSimpleName}", e)
            }
          case _ => // Skip
        }
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19426 topicbasedremotelogmetadatamanager's initial should happen after the broker ready (kip-1197) (#20203).

**Test**
Test modified in RemoteLogMetadataManagerTestUtils.java covering the corrected behaviour.
