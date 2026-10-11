**Commit**: ad2845f715 · **Ticket**: KAFKA-8080

**Subsystem**: streams/src/test/java/org/apache/kafka/streams/tests/ (EosTestClient, EosTestDriver, StreamsEosTest)

**Category**: State not cleaned up

**Root cause**
After the EOS (exactly-once semantics) verification logic was migrated to a unit/integration test harness in a prior PR, the legacy `streams_eos_test.py` system test and its supporting Java helper classes (`EosTestClient`, `EosTestDriver`, `StreamsEosTest`) were left in the repository. The orphaned code continued to be compiled and maintained even though it was no longer invoked, creating a maintenance burden and misleading developers into thinking the system test was still exercising production code paths. The fix deletes all three Java helpers and the Python test file.

**Before**
```java
// Before: dead code still compiled
public class EosTestClient extends SmokeTestUtil {
    static final String APP_ID = "EosTest";
    // ... 225 lines never called
}
```

**After**
```java
// After: file deleted entirely
```

**How to spot**
Search for Java classes under `streams/src/test/java` whose only callers are Python system-test scripts; if the corresponding Python test has been removed or superseded, the Java helpers are orphaned dead code.

**Test**
Confirm the build and all remaining Streams integration tests pass after deleting the three Java files and the Python script.
