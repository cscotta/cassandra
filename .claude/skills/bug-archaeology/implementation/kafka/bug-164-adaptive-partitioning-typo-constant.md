**Commit**: 2329def2ff · **Ticket**: KAFKA-18068
**Subsystem**: clients/src/main/java/org/apache/kafka/clients/producer/ProducerConfig.java
**Category**: Wrong constant / default value

**Root cause**
The public constant `PARTITIONER_ADPATIVE_PARTITIONING_ENABLE_CONFIG` contained a typo ("ADPATIVE" instead of "ADAPTIVE"). User code referencing this constant would compile and run correctly, but anyone searching for `ADAPTIVE` or spelling the constant correctly would fail to find it. Additionally, `KafkaProducer` read the config using the typo constant, so any user who manually passed the string key `"partitioner.adaptive.partitioning.enable"` would find the setting ignored because the internal lookup used the misspelled constant name (the underlying config key string was correct, only the Java constant name was wrong).

**Before/After**
```java
// Before: only the misspelled constant
public static final String PARTITIONER_ADPATIVE_PARTITIONING_ENABLE_CONFIG =
    "partitioner.adaptive.partitioning.enable";

// After: correctly spelled constant is canonical; misspelled kept as deprecated alias
public static final String PARTITIONER_ADAPTIVE_PARTITIONING_ENABLE_CONFIG =
    "partitioner.adaptive.partitioning.enable";
@Deprecated
public static final String PARTITIONER_ADPATIVE_PARTITIONING_ENABLE_CONFIG =
    PARTITIONER_ADAPTIVE_PARTITIONING_ENABLE_CONFIG;
```

**How to spot**
Search for `ADPATIVE` in any Java identifier. The pattern `[A-Z_]*ADPATIVE[A-Z_]*` catches the misspelled constant. Static analysis tools (SpotBugs, PMD, Checkstyle) will not catch this as it is a semantically valid identifier; only a dictionary-aware tool or manual review finds it.

**Test**
No new test added. The fix is a rename with backward-compatible deprecated alias. Existing producer tests cover the adaptive partitioning code path.
