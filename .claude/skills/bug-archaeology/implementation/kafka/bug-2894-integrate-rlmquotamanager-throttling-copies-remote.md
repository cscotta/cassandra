**Commit**: 23fe71d579 · **Ticket**: KAFKA-15265
**Subsystem**: checkstyle/import-control-core.xml
**Category**: Logic error in condition

**Root cause**
- Added the integration of the quota manager to throttle copy requests to the remote storage. Reference KIP-956
- Added unit-tests for the copy throttling logic. Reviewers: Satish Duggana <satishd@apache.org>, Luke Chen <showuon@gmail.com>, Kamal Chandraprakash<kamal.chandraprakash@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
  <allow pkg="org.opentest4j" class="RemoteLogManagerTest"/>
import java.time.Duration;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
    private final ReentrantLock copyQuotaManagerLock = new ReentrantLock(true);
    private final Condition copyQuotaManagerLockCondition = copyQuotaManagerLock.newCondition();
    /**
     * Returns the timeout for the RLM Tasks to wait for the quota to be available
     */
    Duration quotaTimeout() {
        return Duration.ofSeconds(1);
    }
```

**How to spot**
When reviewing checkstyle/import-control-core.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15265: integrate rlmquotamanager for throttling copies to remote storage (#15820).

**Test**
Test modified in RemoteLogManagerTest.java covering the corrected behaviour.
