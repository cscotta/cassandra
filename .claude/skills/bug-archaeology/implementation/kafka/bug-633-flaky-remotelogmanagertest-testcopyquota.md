**Commit**: fffb84345d · **Ticket**: KAFKA-19578
**Subsystem**: storage/src
**Category**: Resource leak

**Root cause**
Found the flaky test on

https://github.com/apache/kafka/actions/runs/19152512773/job/54746814581?pr=20842

Core Issue: Class loading time is unpredictable, total 100ms timeout for
whole process is too short

The Error:
```
Gradle Test Run :storage:test > Gradle Test Executor 121 >
RemoteLogManagerTest > testCopyQuota(boolean) >
"testCopyQuota(boolean).quotaExceeded=false" STARTED
org.apache.kafka.server.log.remote.storage.RemoteLogManagerTest.testCopyQuota(boolean)[1]
failed, log available in
/home/runner/work/kafka/kafka/storage/build/reports/testOutput/org.apache.kafka.server.log.remote.storage.RemoteLogManagerTest.testCopyQuota(boolean)[1].test.stdout

Gradle Test Run :storage:test > Gra...

**Before/After**
```java
// Before
import org.junit.jupiter.api.Disabled;

    @Disabled("KAFKA-19578")
            assertTimeoutPreemptively(Duration.ofMillis(100), () -> task.copyLogSegmentsToRemote(mockLog));

// After
            assertTimeoutPreemptively(Duration.ofMillis(1000), () -> task.copyLogSegmentsToRemote(mockLog));
```

**How to spot**
When reviewing storage/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19578 fix flaky test for remotelogmanagertest.testcopyquota (#20845).

**Test**
Test modified in RemoteLogManagerTest.java covering the corrected behaviour.
