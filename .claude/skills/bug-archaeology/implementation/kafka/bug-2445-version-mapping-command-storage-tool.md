**Commit**: edac19ba50 · **Ticket**: KAFKA-17277
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
As a part of KIP-1022 the following has been implemented in this patch:

A version-mapping command to to look up the corresponding features for a given metadata version. Using the command with no --release-version argument will return the mapping for the latest stable metadata version. This command has been added to the FeatureCommand Tool and the Storage Tool. The storage tools parsing method has been made more modular similar to the feature command tool

Reviewers: Justine Olshan <jolshan@confluent.io>

**Before/After**
```java
// Before

import net.sourceforge.argparse4j.inf.{ArgumentParserException, Namespace}
import org.apache.kafka.server.common.MetadataVersion
    val parser = ArgumentParsers.
      newArgumentParser("kafka-storage", /* defaultHelp */ true, /* prefixChars */ "-", /* fromFilePrefix */ "@").
      description("The Kafka storage tool.")
    val infoParser = subparsers.addParser("info").
      help("Get information about the Kafka log directories on this node.")
    val formatParser = subparsers.addParser("format").
      help("Format the Kafka log directories on this node.")
    subparsers.addParser("random-uuid").help("Print a random UUID.")
    List(infoParser, formatParser).foreach(parser => {

// After
import net.sourceforge.argparse4j.inf.{ArgumentParserException, Namespace, Subparser, Subparsers}
import org.apache.kafka.server.common.{Features, MetadataVersion}
      case "version-mapping" =>
        runVersionMappingCommand(namespace, printStream)
        0


  /**
   * Maps the given release version to the corresponding metadata version
   * and prints the corresponding features.
   *
   * @param namespace       Arguments containing the release version.
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17277: [1/2] add version mapping command to the storage tool and feature command tool (#16973).

**Test**
Test modified in StorageToolTest.scala covering the corrected behaviour.
