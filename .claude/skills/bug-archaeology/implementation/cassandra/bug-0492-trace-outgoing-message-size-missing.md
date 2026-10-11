# Bug 0492: traceOutgoingMessage Prints Zero Message Size

**Commit**: 0a860b9b3e · **Ticket**: CASSANDRA-15687
**Subsystem**: src/java/org/apache/cassandra/net/OutboundConnection.java, src/java/org/apache/cassandra/tracing/Tracing.java
**Category**: Wrong constant / default value

---

## Root Cause

A refactor of the outbound messaging path changed how message serialization was structured: the serialized byte size became known only at the call site in `OutboundConnection` after the size was computed. The `traceOutgoingMessage` method was not updated to accept the pre-computed size, so it had no way to include it in the trace event. The method signature omitted the `serializedSize` parameter, causing trace log output to show no message size (or a zero/placeholder) instead of the actual serialized byte count.

### Before / After

```java
// BEFORE — in Tracing.java
public void traceOutgoingMessage(Message<?> message, InetAddressAndPort sendTo) {
    String logMessage = String.format("Sending %s message to %s",
                                      message.verb(), sendTo);
    ...
}
// Called as:
Tracing.instance.traceOutgoingMessage(next, settings.connectTo);
```

```java
// AFTER — in Tracing.java
public void traceOutgoingMessage(Message<?> message, int serializedSize, InetAddressAndPort sendTo) {
    String logMessage = String.format("Sending %s message to %s message size %d bytes",
                                      message.verb(), sendTo, serializedSize);
    ...
}
// Called as:
Tracing.instance.traceOutgoingMessage(next, messageSize, settings.connectTo);
```

---

## How to Spot

When a refactor separates the computation of a value (here, serialized byte size) from the method that needs it for logging or tracing, check whether downstream callers were updated to pass the newly-available value. Trace and log helpers that simply omit fields rather than computing them themselves are prone to silently dropping diagnostic information.

---

## Test

No test added.
