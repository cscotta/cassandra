**Commit**: 6cb70a831c · **Ticket**: KAFKA-17277
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
This patch belongs to the ongoing efforts of implementing KIP-1022. Added feature-dependencies command to look up dependencies for a given feature version supplied by --feature flag. If the feature is not known or the version not yet defined, we throw an error. Examples :
bin/kafka-feature feature-dependencies --feature transaction.version=2
transaction.version=2 requires:
metadata.version=4 (3.3-IV0) (listing any other version dependencies)

bin/kafka-feature feature-dependencies --feature metadata.version=17
metadata.version=17 (3.7-IV2) has no dependencies

Reviewers: Justine Olshan <jolshan@confluent.io>, Artem Livshits <alivshits@confluent.io>

**Before/After**
```java
// Before
        throw new TerseFailure(s"Unsupported release version '$releaseVersion'. Supported versions are: " +

// After
import scala.jdk.CollectionConverters.{ListHasAsScala, MapHasAsScala}
      case "feature-dependencies" =>
        runFeatureDependenciesCommand(namespace, printStream)
        0

        throw new TerseFailure(s"Unknown release version '$releaseVersion'. Supported versions are: " +
  def runFeatureDependenciesCommand(
    namespace: Namespace,
    printStream: PrintStream
  ): Unit = {
    val featureArgs = Option(namespace.getList[String]("feature")).map(_.asScala.toList).getOrElse(List.empty)

```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17277: [2/2] add feature dependency command to the storage and the feature command tool (#17093).

**Test**
Test modified in StorageToolTest.scala covering the corrected behaviour.
