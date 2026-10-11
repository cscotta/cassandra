**Commit**: 0edf5dbd20 · **Ticket**: KAFKA-16649
**Subsystem**: core/src
**Category**: Race condition / missing synchronization

**Root cause**
Do not acquire the DynamicBrokerConfig lock in DynamicBrokerConfig.removeReconfigurable. It's not
necessary, because the list that these functions are modifying is a thread-safe
CopyOnWriteArrayList. In DynamicBrokerConfig.reloadUpdatedFilesWithoutConfigChange, I changed the
code to use a simple Java forEach rather than a Scala conversion, in order to feel more confident
that concurrent modifications to the List would not have any bad effects here. (forEach is always
safe on CopyOnWriteArrayList.)

Reviewers: Chia-Ping Tsai <chia7712@gmail.com>, Manikumar Reddy <manikumar.reddy@gmail.com>, David Arthur <mumrah@gmail.com>

**Before/After**
```java
// Before
  def addReconfigurable(reconfigurable: Reconfigurable): Unit = CoreUtils.inWriteLock(lock) {
  def addBrokerReconfigurable(reconfigurable: BrokerReconfigurable): Unit = CoreUtils.inWriteLock(lock) {
  def removeReconfigurable(reconfigurable: Reconfigurable): Unit = CoreUtils.inWriteLock(lock) {
  private def verifyReconfigurableConfigs(configNames: Set[String]): Unit = CoreUtils.inWriteLock(lock) {
    reconfigurables.asScala
      .filter(reconfigurable => ReloadableFileConfigs.exists(reconfigurable.reconfigurableConfigs.contains))
      .foreach {
        case reconfigurable: ListenerReconfigurable =>
          val kafkaProps = validatedKafkaProps(newProps, perBrokerConfig = true)
          val newConfig = new KafkaConfig(kafkaProps.asJava, false)
          processListenerReconfigurable(reconfigurable, newConfig, Collections.emptyMap(), validateOnly = false, reloadOnly = true)
        case reconfigurable =>

// After
  def addReconfigurable(reconfigurable: Reconfigurable): Unit = {
  def addBrokerReconfigurable(reconfigurable: BrokerReconfigurable): Unit = {
  def removeReconfigurable(reconfigurable: Reconfigurable): Unit = {
  private def verifyReconfigurableConfigs(configNames: Set[String]): Unit = {
    reconfigurables.forEach(r => {
      if (ReloadableFileConfigs.exists(r.reconfigurableConfigs.contains)) {
        r match {
          case reconfigurable: ListenerReconfigurable =>
            val kafkaProps = validatedKafkaProps(newProps, perBrokerConfig = true)
            val newConfig = new KafkaConfig(kafkaProps.asJava, false)
            processListenerReconfigurable(reconfigurable, newConfig, Collections.emptyMap(), validateOnly = false, reloadOnly = true)
          case reconfigurable =>
```

**How to spot**
When reviewing core/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16649: remove lock from dynamicbrokerconfig.removereconfigurable (#15838).

**Test**
Test modified in KRaftClusterTest.scala covering the corrected behaviour.
