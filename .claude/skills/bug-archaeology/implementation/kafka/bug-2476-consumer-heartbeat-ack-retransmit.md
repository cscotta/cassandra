**Commit**: 28e2e8631f · **Ticket**: KAFKA-17170
**Subsystem**: clients/consumer/internals/HeartbeatRequestManager
**Category**: Race condition / missing synchronization

**Root cause**
When the new consumer sent a heartbeat in RECONCILING state that included an assignment acknowledgement, and that heartbeat timed out before the broker responded, the next heartbeat was built without the ack. The reconciliation was only cleared on a successful response, so a lost heartbeat meant the consumer never re-sent the ack and the coordinator could not advance its reconciliation state. The fix retains the pending ack across timeout retries until a successful response clears it.

**Before/After**
```java
// Before: ack cleared only on success, not replayed on timeout
// HeartbeatRequestManager did not re-include ack if HB response was not received

// After: pending ack is preserved and re-sent on the next heartbeat
// if the previous one timed out without a response
if (pendingAck != null) {
    builder.setAckedAssignment(pendingAck);
}
// pendingAck only cleared after a successful ACK response
```

**How to spot**
When a protocol relies on a request/response round-trip to advance shared state, verify that a timeout or dropped response does not permanently lose the payload. If the sender clears the pending data on send rather than on acknowledged receipt, retries will silently omit that data.

**Test**
New test verifies that a heartbeat in RECONCILING state with a lost response still includes the ack in the subsequent heartbeat.
