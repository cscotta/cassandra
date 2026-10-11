**Commit**: c3d22180d4 · **Ticket**: KAFKA-18128
**Subsystem**: generator/src
**Category**: Wrong serialization / deserialization

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
     * @param filePath   The file to read from remote git repo.
     * @return                     The file contents.
    static String GetDataFromGit(String filePath, Path gitPath) throws IOException {
        Ref head = git.getRepository().getRefDatabase().firstExactRef("refs/heads/trunk");
                return new String(loader.getBytes(), "UTF-8");
import static org.apache.kafka.message.checker.CheckerUtils.GetDataFromGit;
                while (!rootKafkaDirectory.endsWith("kafka")) {
                        throw new RuntimeException("Invalid directory, need to be within the kafka directory");
                String gitContent = GetDataFromGit(filePath, rootKafkaDirectory);
            MetadataSchemaCheckerTool.run(new String[]{"verify-evolution-git", "--file", "AbortTransactionRecord.json"}, new Print

// After
import java.nio.charset.StandardCharsets;
     * @param filePath The file to read from remote git repo.
     * @param ref The specific git reference to be used for testing.
     * @return The file contents.
    static String getDataFromGit(String filePath, Path gitPath, String ref) throws IOException {
        Ref head = repository.getRefDatabase().findRef(ref);
        if (head == null) {
            throw new IllegalStateException("Cannot find " + ref + " in the repository.");
        }

                return new String(loader.getBytes(), StandardCharsets.UTF_8);
import java.nio.file.Files;
```

**How to spot**
When reviewing generator/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18128 fix failed test metadataschemacheckertooltest.testverifyevolutiongit in pr (#17996).

**Test**
Test modified in MetadataSchemaCheckerToolTest.java covering the corrected behaviour.
