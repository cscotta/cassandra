**Commit**: 459da4795a · **Ticket**: KAFKA-16525
**Subsystem**: checkstyle/import-control.xml
**Category**: State not cleaned up

**Root cause**
Allow KRaft replicas to send requests to any node (Node) not just the nodes configured in the
controller.quorum.voters property. This flexibility is needed so KRaft can implement the
controller.quorum.voters configuration, send request to the dynamically changing set of voters and
send request to the leader endpoint (Node) discovered through the KRaft RPCs (specially
BeginQuorumEpoch request and Fetch response). This was achieved by changing the RequestManager API to accept Node instead of just the replica ID. Internally, the request manager tracks connection state using the Node.idString method to match the
connection management used by NetworkClient.

**Before/After**
```java
// Before
  def voterNode(id: Int, listener: String): Option[Node]
      config.controllerListenerNames.head,
    val netClient = buildNetworkClient()
    new KafkaNetworkChannel(time, netClient, config.quorumRequestTimeoutMs, threadNamePrefix)
  private def buildNetworkClient(): NetworkClient = {
    new NetworkClient(
  override def voterNode(id: Int, listener: String): Option[Node] = {

// After
    <allow pkg="org.apache.kafka.common.network" />
import java.util.{Collection => JCollection}
  def voterNode(id: Int, listener: ListenerName): Option[Node]
  bootstrapServers: JCollection[InetSocketAddress],
      bootstrapServers,
    val (listenerName, netClient) = buildNetworkClient()
    new KafkaNetworkChannel(time, listenerName, netClient, config.quorumRequestTimeoutMs, threadNamePrefix)
  private def buildNetworkClient(): (ListenerName, NetworkClient) = {
    val networkClient = new NetworkClient(

    (controllerListenerName, networkClient)
  override def voterNode(id: I
```

**How to spot**
When reviewing checkstyle/import-control.xml code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16525; dynamic kraft network manager and channel (#15986).

**Test**
Test modified in TestRaftServer.scala covering the corrected behaviour.
