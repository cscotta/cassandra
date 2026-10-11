**Commit**: 563338c0e9 · **Ticket**: KAFKA-18711
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Follow-up PR of KAFKA-18711. The motivation and reason for this change
are outlined in [1]. [1] - https://github.com/apache/kafka/pull/20475#discussion_r2375608168

Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.server.config.DelegationTokenManagerConfigs
    else if (!new DelegationTokenManagerConfigs(config).tokenAuthEnabled)

// After
    else if (!tokenManager.isEnabled)
    public boolean isEnabled() {
        return secretKey != null;
    }

```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: minor: refactor on delegationtokenmanager follow up with kafka-18711 (#20579).

**Test**
No test added.
