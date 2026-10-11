**Commit**: a38829e61b · **Ticket**: KAFKA-16765
**Subsystem**: clients/src
**Category**: Race condition / missing synchronization

**Root cause**
Reviewers: Greg Harris <greg.harris@aiven.io>

**Before/After**
```java
// Before
          new DataInputStream(socket.getInputStream).readByte()

// After
                        socket.close();
    public void closeNewChannels() throws IOException {
        for (SocketChannel channel : newChannels) {
            channel.close();
        }
        newChannels.clear();
    }

        closeNewChannels();
          val inputStream = new DataInputStream(socket.getInputStream)
          inputStream.readByte()
          inputStream.close()
```

**How to spot**
When reviewing clients/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16765: close leaked accepted sockets in echoserver, nioechoserver,  servershutdowntest (#16576).

**Test**
Test modified in EchoServer.java covering the corrected behaviour.
