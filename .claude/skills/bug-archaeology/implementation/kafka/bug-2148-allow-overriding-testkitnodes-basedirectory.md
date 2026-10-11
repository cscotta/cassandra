**Commit**: 98b7e4deaf · **Ticket**: KAFKA-17574
**Subsystem**: checkstyle/suppressions.xml
**Category**: Logic error in condition

**Root cause**
This allows shutting down a KafkaClusterTestKit from a JVM shutdown hook without risking error logs because the base directory has already been deleted by the shutdown hook TestUtils.tempDirectory sets up. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before

            String baseDirectory = TestUtils.tempDirectory().getAbsolutePath();
                    baseDirectory,
                    baseDirectory,
            return new TestKitNodes(baseDirectory, clusterId, bootstrapMetadata, controllerNodes, brokerNodes,
import static org.junit.jupiter.api.Assertions.fail;
    public void testCreateClusterAndCloseWithMultipleLogDirs(boolean combined) {

// After
    <suppress checks="NPathComplexity" files="TestKitNodes.java"/>
import java.nio.file.Path;
        private Path baseDirectory;
        public Builder setBaseDirectory(Path baseDirectory) {
            this.baseDirectory = baseDirectory;
            return this;
        }

            if (baseDirectory == null) {
                this.baseDirectory = TestUtils.tempDirectory().toPath();
            }
                    baseDirectory.toFile().getAbsolutePath(),
```

**How to spot**
When reviewing checkstyle/suppressions.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17574 allow overriding testkitnodes basedirectory (#17225).

**Test**
Test modified in TestKitNodes.java covering the corrected behaviour.
