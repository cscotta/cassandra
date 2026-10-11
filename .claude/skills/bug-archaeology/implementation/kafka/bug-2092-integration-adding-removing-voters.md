**Commit**: ebb3202e01 · **Ticket**: KAFKA-16964
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
class SocketServer(val config: KafkaConfig,
                   val metrics: Metrics,
                   val time: Time,
                   val credentialProvider: CredentialProvider,
                   val apiVersionManager: ApiVersionManager,
                   val connectionDisconnectListeners: Seq[ConnectionDisconnectListener] = Seq.empty)
  extends Logging with BrokerReconfigurable {
    val socketAddress =
      if (Utils.isBlank(host))
        new InetSocketAddress(port)
      else
        new InetSocketAddress(host, port)

// After
import org.apache.kafka.server.ServerSocketFactory
class SocketServer(
  val config: KafkaConfig,
  val metrics: Metrics,
  val time: Time,
  val credentialProvider: CredentialProvider,
  val apiVersionManager: ApiVersionManager,
  val socketFactory: ServerSocketFactory = ServerSocketFactory.INSTANCE,
  val connectionDisconnectListeners: Seq[ConnectionDisconnectListener] = Seq.empty
) extends Logging with BrokerReconfigurable {
    val socketAddress = if (Utils.isBlank(host)) {
      new InetSocketAddress(port)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16964 integration tests for adding and removing voters (#17582).

**Test**
Test modified in ReconfigurableQuorumIntegrationTest.java covering the corrected behaviour.
