**Commit**: 22b89b6413 · **Ticket**: KAFKA-19192
**Subsystem**: metadata/src
**Category**: Logic error in condition

**Root cause**
Old bootstrap.metadata files cause problems with server that include
KAFKA-18601. When the server tries to read the bootstrap.checkpoint
file, it will fail if the metadata.version is older than 3.3-IV3
(feature level 7). This causes problems when these clusters are
upgraded. This PR makes it possible to represent older MVs in BootstrapMetadata
objects without causing an exception.

**Before/After**
```java
// Before
    private final MetadataVersion metadataVersion;
        return new BootstrapMetadata(records, metadataVersion, source);
        return new BootstrapMetadata(records, metadataVersion, source);
        MetadataVersion metadataVersion = null;
            Optional<MetadataVersion> version = recordToMetadataVersion(record.message());
            if (version.isPresent()) {
                metadataVersion = version.get();
        if (metadataVersion == null) {
        return new BootstrapMetadata(records, metadataV

// After
import scala.util.Using
  /**
   * Test that once a cluster is formatted, a bootstrap.metadata file that contains an unsupported
   * MetadataVersion is not a problem. This is a regression test for KAFKA-19192.
   */
  @Test
  def testOldBootstrapMetadataFile(): Unit = {
    val baseDirectory = TestUtils.tempDir().toPath()
    Using.resource(new KafkaClusterTestKit.Builder(
      new TestKitNodes.Builder().
        setNumBrokerNodes(1).
        setNumControllerNodes(1).
```

**How to spot**
When reviewing metadata/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19192; old bootstrap checkpoint files cause problems updated servers (#19545).

**Test**
Test modified in KRaftClusterTest.scala covering the corrected behaviour.
