**Commit**: 79fe1305b6 · **Ticket**: KAFKA-18893
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
ReplicaSelector implementations can implement Monitorable to register their own metrics. Reviewers: Mickael Maison <mickael.maison@gmail.com>, Ken Huang <s7133700@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.common.internals.Topic
  private[server] val replicaSelectorOpt: Option[ReplicaSelector] = createReplicaSelector()
          replicaSelectorOpt.foreach { selector =>
            debug(s"Replica selector ${selector.getClass.getSimpleName} returned preferred replica " +
        replicaSelectorOpt.flatMap { replicaSelector =>
          replicaSelector.select(partition.topicPartition, clientMetadata, partitionInfo).toScala.collect {
    replicaSelectorOpt.foreach(_.close)
  private def createReplicaSelector(): Option[ReplicaSelector] = {
      tmpReplicaSelector

// After
 * Implement {@link org.apache.kafka.common.metrics.Monitorable} to enable the selector to register metrics.
 * The following tags are automatically added to all metrics registered: <code>config</code> set to
 * <code>replica.selector.class</code>, and <code>class</code> set to the ReplicaSelector class name.
import org.apache.kafka.common.internals.{Plugin, Topic}
  private[server] val replicaSelectorPlugin: Option[Plugin[ReplicaSelector]] = createReplicaSelector(metrics)
          replicaSelectorPlugin.foreach { selector =>
            debug(s"Replica selector ${selector.get.getClass.getSimpleName} returned preferred replica " +
        replicaSelectorPlugin.flatMap { replicaSelector =>
          replicaSelector.get.select(partition.topicPartition, clientMetadata, partitionInfo).toScala.collect {
    replicaSelectorPlugin.foreach(_.close)
  private def createReplicaSelector(metrics: Metrics): Option[Plugin[ReplicaSelector]] = {
      Plugin.wrapInstance(tmpReplicaSelector, metrics, className)
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18893: add kip-877 support to replicaselector (#19064).

**Test**
Test modified in ReplicaManagerTest.scala covering the corrected behaviour.
