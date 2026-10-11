# Bug 1988: Deadlock in Streaming 2.0 When Both Sides Use the Same Socket for Incoming and Outgoing

**Commit**: 67ccdabfe8 · **Ticket**: CASSANDRA-5699
**Subsystem**: src/java/org/apache/cassandra/streaming/ConnectionHandler.java, src/java/org/apache/cassandra/streaming/StreamSession.java, src/java/org/apache/cassandra/streaming/StreamResultFuture.java, src/java/org/apache/cassandra/net/IncomingStreamingConnection.java
**Category**: Deadlock

---

## Root Cause

The original Streaming 2.0 implementation attempted to share a single blocking socket for both incoming and outgoing data between two nodes. Because both streams ran on the same socket and both sides could be trying to write at the same time (e.g., sending a prepare message while waiting to receive one), the blocking I/O operations on the shared socket could mutually block each other, resulting in a deadlock.

The fix separates the concerns: each peer uses a dedicated socket for outgoing data and receives on a separate incoming socket. The initiator opens a connection (its outgoing socket), sends a `StreamInitMessage` with `sentByInitiator=true`, and then waits. The receiving side opens its own back-connection (its outgoing socket), sends a `StreamInitMessage` with `sentByInitiator=false`, and starts the incoming handler on the original socket. The initiator, upon receiving this second init message, attaches its incoming socket and begins actual streaming. This ensures no two threads compete for the same blocking socket, eliminating the deadlock.

### Before / After

```java
// BEFORE: single socket passed into both IncomingMessageHandler and OutgoingMessageHandler
incoming = new IncomingMessageHandler(session, protocolVersion, in);
outgoing = new OutgoingMessageHandler(session, protocolVersion, out);
// both use the same socket channel — deadlock under concurrent writes/reads
```

```java
// AFTER: initiator and receiver each open their own outgoing socket
public ConnectionHandler initiate() throws IOException
{
    outgoing = new OutgoingMessageHandler(session, connect(session.peer), ...);
    outgoing.sendInitMessage(true);
    outgoing.start();
    return this;
}

public ConnectionHandler initiateOnReceivingSide(Socket incomingSocket, int version) throws IOException
{
    incoming = new IncomingMessageHandler(session, incomingSocket, version);
    incoming.start();
    outgoing = new OutgoingMessageHandler(session, connect(session.peer), version);
    outgoing.sendInitMessage(false);
    outgoing.start();
    return this;
}
```

---

## How to Spot

Whenever a blocking socket is used for bidirectional communication managed by two separate threads, look for the possibility that both threads can block simultaneously waiting for the other side—a classic two-thread deadlock. Protocols that require a handshake before data transfer are especially prone to this: if A sends a message while waiting for B's acknowledgement, and B is simultaneously doing the same on the same socket, neither side progresses.

---

## Test

No new unit test added; the fix is validated by the existing streaming integration tests.
