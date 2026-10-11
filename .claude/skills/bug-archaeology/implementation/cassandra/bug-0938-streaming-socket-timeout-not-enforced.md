# Bug 0938: streaming_socket_timeout_in_ms not enforced

**Commit**: 561000aa30 · **Ticket**: CASSANDRA-11286
**Subsystem**: Streaming / Network
**Category**: Wrong constant / default value

---

## Root Cause
Two independent problems prevented `streaming_socket_timeout_in_ms` from taking effect. First, `ConnectionHandler.getReadChannel()` preferred `socket.getChannel()` (a raw NIO `SocketChannel`) over `Channels.newChannel(socket.getInputStream())`; `SO_TIMEOUT` is honoured only on the blocking stream API, not on `SocketChannel`. Second, the timeout was applied only on the initiating side and never set on the follower/incoming side in `IncomingStreamingConnection`. Additionally, when a streaming side completed its work and transitioned to `WAIT_COMPLETE`, it did not close its half of the connection, so the other side could block forever waiting for data that would never arrive.

### Before / After
```java
// BEFORE — getReadChannel used raw NIO channel, ignoring SO_TIMEOUT
ReadableByteChannel in = socket.getChannel();
return in == null ? Channels.newChannel(socket.getInputStream()) : in;

// AFTER — always use InputStream so SO_TIMEOUT is respected
return Channels.newChannel(socket.getInputStream());
```

```java
// BEFORE — follower never set SO_TIMEOUT
StreamInitMessage init = StreamInitMessage.serializer.deserialize(input, version);

// AFTER — follower sets SO_TIMEOUT on incoming connection
if (!init.isForOutgoing)
    socket.setSoTimeout(DatabaseDescriptor.getStreamingSocketTimeout());
```

---

## How to Spot
Whenever a socket timeout config option is introduced, verify that (a) the socket is read via the blocking `InputStream` path (not a raw `SocketChannel`), and (b) the timeout is applied on both sides of a bidirectional handshake. Also check that half-closed states properly close the dormant half so the peer does not block waiting on a channel that will never send more data.

---

## Test
No test added.
