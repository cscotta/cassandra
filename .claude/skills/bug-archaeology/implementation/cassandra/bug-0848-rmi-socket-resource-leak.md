# Bug 0848: RMIServerSocketFactoryImpl Leaks Socket on setReuseAddress Failure

**Commit**: 19245e5fac · **Ticket**: CASSANDRA-12331
**Subsystem**: JMX / RMI
**Category**: Resource leak

---

## Root Cause
`RMIServerSocketFactoryImpl.createServerSocket()` called `ServerSocketFactory.createServerSocket()` to allocate a `ServerSocket`, then immediately called `socket.setReuseAddress(true)`.  If `setReuseAddress` threw a `SocketException`, the method propagated the exception without first closing the socket, leaving a file-descriptor leak every time JMX initialisation failed partway.

### Before / After
```java
// BEFORE
ServerSocket socket = ServerSocketFactory.getDefault()
                                         .createServerSocket(pPort, 0, InetAddress.getLoopbackAddress());
socket.setReuseAddress(true);
return socket;

// AFTER
ServerSocket socket = ServerSocketFactory.getDefault()
                                         .createServerSocket(pPort, 0, InetAddress.getLoopbackAddress());
try
{
    socket.setReuseAddress(true);
    return socket;
}
catch (SocketException e)
{
    socket.close();
    throw e;
}
```

---

## How to Spot
Whenever a resource is allocated before a configuration call that can itself throw a checked exception, every exception path between allocation and the final `return` must either close/release the resource or hand it to a try-with-resources block.  In this case the `ServerSocket` was created outside any `try` statement, so the single-line `setReuseAddress` call was the only opportunity for a leak — easy to miss because `SocketException` from a socket option setter is rare in practice.

---

## Test
No test added.
