**Commit**: 5791397d03 · **Ticket**: KAFKA-17776
**Subsystem**: clients/src
**Category**: State not cleaned up

**Root cause**
As of now, there isn't any way that resources can be cleaned in Kafka when client disconnects the connections. Generally time bound clean up happens which keep the resources occupied. This PR provides capability to attach listeners in Socket Server which can react to client connection disconnections. Reviewers: Jun Rao <junrao@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    public static String generateConnectionId(Socket socket, int connectionIndex) {
        return localHost + ":" + localPort + "-" + remoteHost + ":" + remotePort + "-" + connectionIndex;
        String connectionId = Selector.generateConnectionId(channel.socket(), nextConnectionIndex);
                   val apiVersionManager: ApiVersionManager)
      val processor = newProcessor(socketServer.nextProcessorId(), listenerName, securityProtocol)
  def newProcessor(id: Int, listenerName: ListenerName, securityProtocol: SecurityProtocol): Processor = {

// After
     * @param processorId The ID of the server processor that will handle this connection.
    public static String generateConnectionId(Socket socket, int processorId, int connectionIndex) {
        return localHost + ":" + localPort + "-" + remoteHost + ":" + remotePort + "-" + processorId + "-" + connectionIndex;
    public String connectionId() {
        return connectionId;
    }

        String connectionId = Selector.generateConnectionId(channel.socket(), 0, nextConnectionIndex);
import org.apache.kafka.server.network.ConnectionDisconnectListener
                   val apiVersionManager: ApiVersionManager,
                   val connectionDisconnectListeners: Seq[ConnectionDisconnectListener] = Seq.empty)
      val processor = newProcessor(socketServer.nextProcessorId(), listenerName, securityProtocol, socketServer.connectionDisconnectListeners)
```

**How to spot**
When reviewing clients/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17776 capability for connection disconnect listeners in socket server (#17473).

**Test**
Test modified in NioEchoServer.java covering the corrected behaviour.
