**Commit**: 2c82ecd67f · **Ticket**: KAFKA-16807
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import java.util.{Optional, OptionalInt, OptionalLong}
        logsByDir.get(absolutePath) match {
            val topicInfos = logs.groupBy(_.topicPartition.topic).map{case (topic, logs) =>
            }.toList.asJava

            new DescribeLogDirsResponseData.DescribeLogDirsResult().setLogDir(absolutePath)
              .setErrorCode(Errors.NONE.code).setTopics(topicInfos)
              .setTotalBytes(totalBytes).setUsableBytes(usableBytes)
            new DescribeLogDirsResponseData.DescribeLogDirsResult().setLogDir(absolutePath)
              .setErrorCode(Errors.NONE.code)
              .setTotalBytes(totalBytes).setUsableBytes(usableBytes)

// After
import org.apache.kafka.common.message.DescribeLogDirsResponseData.DescribeLogDirsTopic
import java.util.{Collections, Optional, OptionalInt, OptionalLong}
        val topicInfos = logsByDir.get(absolutePath) match {
            logs.groupBy(_.topicPartition.topic).map { case (topic, logs) =>
            }.filterNot(_.partitions().isEmpty).toList.asJava
            Collections.emptyList[DescribeLogDirsTopic]()
        val describeLogDirsResult = new DescribeLogDirsResponseData.DescribeLogDirsResult()
          .setLogDir(absolutePath).setTopics(topicInfos)
          .setErrorCode(Errors.NONE.code)
          .setTotalBytes(totalBytes).setUsableBytes(usableBytes)
        if (!topicInfos.isEmpty)
          describeLogDirsResult.setTopics(topicInfos)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16807 describelogdirsresponsedata#results#topics have unexpected topics having empty partitions (#16042).

**Test**
Test modified in ReplicaManagerTest.scala covering the corrected behaviour.
