**Commit**: 5da3b9410d · **Ticket**: KAFKA-17778
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
The ClientsMetricManager keeps the client instance cache at a limit of 16384. The active connections can be low, but connections can be created and destroyed in short span. Which hits the cache limit and results in cache miss for new connections. The client instance cache keeps the instances for 3 * push interval ms.

**Before/After**
```java
// Before
      socketServer = new SocketServer(config, metrics, time, credentialProvider, apiVersionManager)
                clientInstance = createClientInstanceAndUpdateCache(clientInstanceId, instanceMetadata);

// After
      val connectionDisconnectListeners = Seq(clientMetricsManager.connectionDisconnectListener())
      socketServer = new SocketServer(config, metrics, time, credentialProvider, apiVersionManager, connectionDisconnectListeners)
import org.apache.kafka.server.network.ConnectionDisconnectListener;
    private final Map<String, Uuid> clientConnectionIdMap;
    private final ConnectionDisconnectListener connectionDisconnectListener;
        this.clientConnectionIdMap = new ConcurrentHashMap<>();
        this.connectionDisconnectListener = new ClientConnectionDisconnectListener();
    public ConnectionDisconnectListener connectionDisconnectListener() {
        return connectionDisconnectListener;
    }

                clientInstance = createClientInstanceAndUpdateCache(clientInstanceId, instanceMetadata, requestContext.connectionId());
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17778: add listener to remove client instances on connection disconnect (#17474).

**Test**
Test modified in ClientMetricsManagerTest.java covering the corrected behaviour.
