**Commit**: 05b6e81688 · **Ticket**: KAFKA-19420
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Refactor the code related to SocketServer  SocketServer is an internal
class, and normally the integration tests should not use it directly. [KAFKA-19239](https://issues.apache.org/jira/browse/KAFKA-19239) will
add a new helper to expose the bound ports, and so the tests that need
to send raw request can leverage it without accessing the SocketServer. Reviewers: PoAn Yang <payang@apache.org>, Ken Huang
 <s7133700@gmail.com>, TengYao Chi <kitingiao@gmail.com>, Chia-Ping Tsai
 <chia7712@gmail.com>

**Before/After**
```java
// Before
import kafka.network.SocketServer
import org.apache.kafka.common.network.ListenerName
    val ids = clusterInstance.brokerSocketServers().stream().flatMap( broker => {
      IntStream.range(0, 1001).parallel().mapToObj( _ =>
        nextProducerId(broker, clusterInstance.clientListener())
  private def nextProducerId(broker: SocketServer, listener: ListenerName): Long = {
      val port = broker.boundPort(listener)
  def sendApiVersionsRequest(request: ApiVersionsRequest, listenerName: ListenerName): ApiVersionsResponse = {
    val socket = if (cluster.controllerListenerName() == listenerName) {
      cluster.controllerSocketServers().asScala.head
    } else {
      cluster.brokerSocketServers().asScala.head

// After
    val ids = clusterInstance.brokers().values().stream().flatMap(broker => {
      IntStream.range(0, 1001).parallel().mapToObj(_ =>
        nextProducerId(broker.boundPort(clusterInstance.clientListener()))
  private def nextProducerId(port: Int): Long = {
import org.apache.kafka.common.requests.{ApiVersionsRequest, ApiVersionsResponse}
import org.apache.kafka.server.IntegrationTestUtils
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19420 don't export socketserver from clusterinstance (#20002).

**Test**
Test modified in ProducerIntegrationTest.scala covering the corrected behaviour.
