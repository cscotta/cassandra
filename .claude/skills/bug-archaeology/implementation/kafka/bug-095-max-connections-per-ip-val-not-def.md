**Commit**: 832dfa36da · **Ticket**: KAFKA-18637
**Subsystem**: core/src/main/scala/kafka/server/KafkaConfig.scala
**Category**: State not cleaned up

**Root cause**: `maxConnectionsPerIp` and `maxConnectionsPerIpOverrides` were declared as `val` (evaluated once at construction time) rather than `def` (evaluated on each access). Dynamic broker reconfiguration updates the underlying config map, but the stale `val` fields continued to return the original values, so live connection-limit changes had no effect. Changing them to `def` ensures each access reads the current value from the config store.

**Before/After**

```scala
// Before – evaluated once, ignores dynamic updates
val maxConnectionsPerIp = getInt(SocketServerConfigs.MAX_CONNECTIONS_PER_IP_CONFIG)
val maxConnectionsPerIpOverrides: Map[String, Int] = getMap(...)

// After – re-evaluated on each call
def maxConnectionsPerIp = getInt(SocketServerConfigs.MAX_CONNECTIONS_PER_IP_CONFIG)
def maxConnectionsPerIpOverrides: Map[String, Int] = getMap(...)
```

**How to spot**: In Scala, `val` fields that call mutable config accessors will cache the initial value. Any config property that must reflect dynamic reconfiguration must use `def`. Compare against similar properties in the same class (e.g. `def maxConnections`, `def maxConnectionCreationRate`) which already use `def`.

**Test**: `testSocketServerConfigTest` — dynamically updates `MAX_CONNECTIONS_PER_IP_CONFIG` and `MAX_CONNECTIONS_PER_IP_OVERRIDES_CONFIG`, restarts the broker, and asserts the new values are present in `broker.config.originals()`.
