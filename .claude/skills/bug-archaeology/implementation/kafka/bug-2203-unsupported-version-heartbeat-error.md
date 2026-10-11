**Commit**: 8e54a04723 · **Ticket**: KAFKA-17536
**Subsystem**: clients/consumer (AbstractHeartbeatRequestManager)
**Category**: Silent skip on non-match

**Root cause**
When a broker returned `UNSUPPORTED_VERSION` to a consumer heartbeat, the error was swallowed by the generic error handler and no actionable message was surfaced to the user. The consumer silently stopped making progress without guidance on what to do, because the specific `UNSUPPORTED_VERSION` case was never extracted from the catch-all handler.

**Before/After**
```java
// Before – UNSUPPORTED_VERSION fell into generic handler
default:
    log.error("Unexpected error in heartbeat: {}", error);

// After – dedicated case with remediation hint
case UNSUPPORTED_VERSION:
    log.error("Consumer group protocol is not supported. "
        + "Set group.protocol=classic to use the classic protocol.");
    break;
```

**How to spot**
For every network response handler, enumerate the protocol error codes that require special user-visible remediation (protocol mismatches, missing features) and ensure each has an explicit case with a diagnostic message rather than falling through to a generic log line.

**Test**
No test added.
