**Commit**: ccd24f2cf6 · **Ticket**: KAFKA-17341
**Subsystem**: clients/consumer/internals/HeartbeatRequestManager
**Category**: Missing override of default method

**Root cause**
`HeartbeatRequestManager` and `ShareHeartbeatRequestManager` had almost identical heartbeat request/response handling logic that was duplicated rather than shared. This violated DRY and meant bug fixes in one class were not automatically applied to the other. The fix extracts the common logic into `AbstractHeartbeatRequestManager<R extends AbstractResponse>` and makes `membershipManager()`, `buildHeartbeatRequest()`, `errorForResponse()`, and related methods abstract, forcing each subclass to supply only the protocol-specific parts.

**Before/After**
```java
// Before: HeartbeatRequestManager was a concrete class with
// all logic inlined for the consumer group protocol only

// After: abstract base class with type parameter
public abstract class AbstractHeartbeatRequestManager<R extends AbstractResponse>
        implements RequestManager {
    public abstract AbstractMembershipManager<R> membershipManager();
    protected abstract NetworkClientDelegate.UnsentRequest buildHeartbeatRequest();
    protected abstract Errors errorForResponse(R response);
}
```

**How to spot**
When two classes share nearly identical logic that differs only in the request/response type, introduce a generic abstract base class rather than copying code. A copy/paste discrepancy between the two classes is a latent bug waiting to surface when one class is fixed and the other is not.

**Test**
`HeartbeatRequestManagerTest` refactored to exercise the abstract base; `ShareHeartbeatRequestManagerTest` reuses the same test harness.
