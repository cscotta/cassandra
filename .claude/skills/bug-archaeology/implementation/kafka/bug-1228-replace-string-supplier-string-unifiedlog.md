**Commit**: e69a311068 · **Ticket**: KAFKA-19076
**Subsystem**: storage/src
**Category**: Race condition / missing synchronization

**Root cause**
jira: https://issues.apache.org/jira/browse/KAFKA-19076

the message is used when the function encounters error, so the error
message should be created lazy. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
                    "Error while renaming dir for " + topicPartition() + " in dir " + dir().getParent(),
                    "Error while renaming dir for " + topicPartition() + " in log dir " + dir().getParent(),
                        "Error while appending records to " + topicPartition() + " in dir " + dir().getParent(),
                "Exception while increasing log start offset for " + topicPartition() + " to " + newLogStartOffset + " in dir " + dir().getParent(),
                "Error while fetching offset by timestamp for " + topicPartition() + " in dir " + dir().getParent(),
        return maybeHandleIOException("Error while deleting segments for " + topicPartition() + " in dir " + dir().getParent(),
                "Error while flushing log for " + topicPartition() + " in dir " + dir().getParent() + " with offset " + offset +
            "Error while deleting log for " + topicPartition() + " in dir " + dir().getParent(),

// After
import java.util.function.Supplier;
                    () -> "Error while renaming dir for " + topicPartition() + " in dir " + dir().getParent(),
                    () -> "Error while renaming dir for " + topicPartition() + " in log dir " + dir().getParent(),
                        () -> "Error while appending records to " + topicPartition() + " in dir " + dir().getParent(),
                () -> "Exception while increasing log start offset for " + topicPartition() + " to " + newLogStartOffset + " in dir " + dir().getParent(),
                () -> "Error while fetching offset by timestamp for " + topicPartition() + " in dir " + dir().getParent(),
        return maybeHandleIOException(() -> "Error while deleting segments for " + topicPartition() + " in dir " + dir().getParent(),
                () -> "Error while flushing log for " + topicPartition() + " in dir " + dir().getParent() + " with offset " + offset +
            () -> "Error while deleting log for
```

**How to spot**
When reviewing storage/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19076 replace `string` by `supplier<string>` for unifiedlog#maybehandleioexception (#19392).

**Test**
No test added.
