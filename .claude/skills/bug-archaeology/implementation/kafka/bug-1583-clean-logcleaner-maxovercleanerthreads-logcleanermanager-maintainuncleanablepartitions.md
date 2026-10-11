**Commit**: 697cf641bc · **Ticket**: KAFKA-17668
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Since maxOverCleanerThreads does not have a corresponding unit test,
I have added a unit test for it. maintainUncleanablePartitions has been
thoroughly tested in tests, so I simply replaced the old implementation
with the new one. Reviewers: Ismael Juma <ismael@juma.me.uk>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
   * scala 2.12 does not support maxOption so we handle the empty manually.
  private def maxOverCleanerThreads(f: CleanerThread => Double): Int =
    cleaners.foldLeft(0.0d)((max: Double, thread: CleanerThread) => math.max(max, f(thread))).toInt
      // Note: we don't use retain or filterInPlace method in this function because retain is deprecated in
      // scala 2.13 while filterInPlace is not available in scala 2.12.

      uncleanablePartitions.values.foreach {
        partitions =>
          val partitionsToRemove = partitions.filterNot(logs.contains).toList
          partitionsToRemove.foreach { partitions.remove }
      val logDirsToRemove = uncleanablePartitions.filter {
        case (_, partitions) => partitions.isEmpty

// After
  private[log] def maxOverCleanerThreads(f: CleanerThread => Double): Int =
    cleaners.map(f).maxOption.getOrElse(0.0d).toInt
      uncleanablePartitions.values.foreach { partitions =>
        partitions.filterInPlace(logs.contains)
      uncleanablePartitions.filterInPlace {
        case (_, partitions) => partitions.nonEmpty
      }
  @Test
  def testMaxOverCleanerThreads(): Unit = {
    val logCleaner =  new LogCleaner(new CleanerConfig(true),
      logDirs = Array(TestUtils.tempDir(), TestUtils.tempDir()),
      logs = new Pool[TopicPartition, UnifiedLog](),
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17668: clean-up logcleaner#maxovercleanerthreads and logcleanermanager#maintainuncleanablepartitions (#17390).

**Test**
Test modified in LogCleanerTest.scala covering the corrected behaviour.
