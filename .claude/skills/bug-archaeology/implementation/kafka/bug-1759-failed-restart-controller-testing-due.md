**Commit**: c7c1364b0f · **Ticket**: KAFKA-18274
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.common.metrics.Metrics
      //  metrics will be set to null when closing a controller, so we should recreate it for testing
      controller.sharedServer.metrics = new Metrics()

// After
import org.apache.kafka.common.metrics.Metrics
      //  metrics will be set to null when closing a controller, so we should recreate it for testing
      if (sharedServer.metrics == null){
        sharedServer.metrics = new Metrics()
      }

```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18274 failed to restart controller in testing due to closed socket channel [1/2] (#18310).

**Test**
Test modified in KRaftClusterTest.scala covering the corrected behaviour.
