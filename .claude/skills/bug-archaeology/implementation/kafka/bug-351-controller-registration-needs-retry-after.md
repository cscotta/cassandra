**Commit**: 32c0e1c4d0 · **Ticket**: KAFKA-20247
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
Set pendingRpc to false when controller registration times out. If we do
not set this, controllers cannot send ControllerRegistrationRequests
thereafter. Instead, subsequent calls to
maybeSendControllerRegistration() will always log:
"maybeSendControllerRegistration: waiting for the previous RPC to
complete." The asserts on L294 and L300 fail when pendingRpc does not
get set in onTimeout. Previously, RegistrationResponseHandler callbacks were invoked from the
NodeToControllerRequestThread.

**Before/After**
```java
// Before
    override def onTimeout(): Unit = {
import org.apache.kafka.common.utils.{ExponentialBackoff, Time}
      Time.SYSTEM,
      new ExponentialBackoff(1, 2, 100, 0.02))

// After
      eventQueue.append(new RequestCompleteEvent(response))
    }

    override def onTimeout(): Unit = {
      eventQueue.append(new RequestTimeoutEvent())
    }
  }

  private class RequestCompleteEvent(response: ClientResponse) extends EventQueue.Event {
    override def run(): Unit = {
  }
  private class RequestTimeoutEvent extends EventQueue.Event {
```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20247; controller registration needs to retry after request timeout (#21619).

**Test**
Test modified in ControllerRegistrationManagerTest.scala covering the corrected behaviour.
