**Commit**: 3fcaec7f4e · **Ticket**: KAFKA-18399
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Christo Lolov <lolovc@amazon.com>, Ismael Juma <ismael@juma.me.uk>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.clients.admin.AlterConfigOp.OpType
import org.apache.kafka.clients.admin.{AlterConfigOp, ConfigEntry, EndpointType}
import org.apache.kafka.common.config.ConfigResource
import org.apache.kafka.common.message.AlterConfigsResponseData.AlterConfigsResourceResponse
    } else if ((!request.isForwarded) && metadataSupport.canForward()) {
    } else {
      sendResponse(Some(processLegacyAlterConfigsRequest(request, remaining)))
    }
  }

  def processLegacyAlterConfigsRequest(
    originalRequest: RequestChannel.Request,

// After
import org.apache.kafka.clients.admin.EndpointType
    } else {
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18399 remove zookeeper from kafkaapis (10/n): alter_config and incremenetal_alter_config (#18432).

**Test**
Test modified in KafkaApisTest.scala covering the corrected behaviour.
