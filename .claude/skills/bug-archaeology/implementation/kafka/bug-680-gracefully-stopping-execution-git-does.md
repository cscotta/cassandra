**Commit**: 6f1bd8fc63 · **Ticket**: KAFKA-18973
**Subsystem**: generator/src
**Category**: Wrong serialization / deserialization

**Root cause**
The test logic covers three main scenarios:

1. Running from the Kafka Root Directory

    Action: The test immediately finds the .git directory. Result: It uses this location to correctly find the schema file and
runs the test successfully. 2.

**Before/After**
```java
// Before
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            Path rootKafkaDirectory = Paths.get("").toAbsolutePath();
            while (!Files.exists(rootKafkaDirectory.resolve(".git"))) {
                rootKafkaDirectory = rootKafkaDirectory.getParent();
                if (rootKafkaDirectory == null) {
                    throw new RuntimeException("Invalid directory, need to be within a Git repository");
                }

// After
import static org.junit.jupiter.api.Assumptions.assumeTrue;
        // Try to find the Git root directory
        Path rootKafkaDirectory = Paths.get("").toAbsolutePath();
        boolean gitFound = false;

        while (rootKafkaDirectory != null) {
            if (Files.exists(rootKafkaDirectory.resolve(".git"))) {
                gitFound = true;
                break;
            rootKafkaDirectory = rootKafkaDirectory.getParent();
        }

```

**How to spot**
When reviewing generator/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18973  gracefully stopping test execution if .git does not exist (#20703).

**Test**
Test modified in MetadataSchemaCheckerToolTest.java covering the corrected behaviour.
