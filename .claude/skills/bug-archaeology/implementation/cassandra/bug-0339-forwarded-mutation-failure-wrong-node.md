# Bug 0339: Forwarded Mutation Failure Callbacks Sent to Wrong Node

**Commit**: 7f54fe0229 · **Ticket**: CASSANDRA-16334
**Subsystem**: src/java/org/apache/cassandra/net/MessageDeliveryTask.java
**Category**: Logic error in condition

---

## Root Cause

When a coordinator forwards a mutation to a replica and that replica in turn forwards it to additional replicas, a failure on the innermost replica would call `MessagingService.sendReply(failureResponse, id, message.from)`. Here `message.from` is the intermediate forwarding node, not the original coordinator who owns the request ID and is waiting for the callback. The original sender's address is stored in the `FORWARD_FROM` message parameter, but `doFailureCallback` ignored it and always replied to `message.from`. The result was that write failure responses were routed back to the intermediate forwarder (which discarded them as unsolicited), and the original coordinator's write handler never received the failure notification, leaving it stuck waiting until timeout. A secondary issue in `AbstractWriteResponseHandler.totalEndpoints()` also failed to restrict the endpoint count to the local datacenter for DC-local consistency levels.

### Before / After

```java
// BEFORE – always sends failure reply to the direct sender, not the originator
MessagingService.instance().sendReply(response, id, message.from);
```

```java
// AFTER – uses FORWARD_FROM if present to route the reply to the true originator
byte[] fromBytes = message.parameters.get(Mutation.FORWARD_FROM);
InetAddress from = fromBytes != null ? InetAddress.getByAddress(fromBytes) : message.from;
MessagingService.instance().sendReply(response, id, from);
```

---

## How to Spot

In any multi-hop request routing system, the reply target for failure callbacks must be resolved from the request origin (stored in a parameter), not from the transport-level `from` field, which only represents the immediately-previous hop. Review all `doFailureCallback` / error-reply paths to confirm they resolve the ultimate originator rather than the last forwarder.

---

## Test

No new test added; the bug was reproduced by tracing failure notification paths across forwarded mutations.
