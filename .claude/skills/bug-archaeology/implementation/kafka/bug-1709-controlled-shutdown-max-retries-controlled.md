**Commit**: f95726a211 · **Ticket**: KAFKA-18417
**Subsystem**: server/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
      configProps.put("controlled.shutdown.max.retries", "1")
      configProps.put("controlled.shutdown.retry.backoff.ms", "1000")
        case ServerConfigs.CONTROLLED_SHUTDOWN_MAX_RETRIES_CONFIG => assertPropertyInvalid(baseProperties, name, "not_a_number")
        case ServerConfigs.CONTROLLED_SHUTDOWN_RETRY_BACKOFF_MS_CONFIG => assertPropertyInvalid(baseProperties, name, "not_a_number")
    props.put(ServerConfigs.CONTROLLED_SHUTDOWN_RETRY_BACKOFF_MS_CONFIG, "100")
    public static final String CONTROLLED_SHUTDOWN_MAX_RETRIES_CONFIG = "controlled.shutdown.max.retries";
    public static final int CONTROLLED_SHUTDOWN_MAX_RETRIES_DEFAULT = 3;
    public static final String CONTROLLED_SHUTDOWN_MAX_RETRIES_DOC = "Controlled shutdown can fail for multiple reasons. This determines the number of retries when such failure happens";

    public static final String CONTROLLED_SHUTDOWN_RETRY_BACKOFF_MS_CONFIG = "controlled.shutdown.retry.backoff.ms";
    public static final int CONTROLLED_SHUTDOWN_RETRY_BACKOFF_MS_DEFAULT = 5000;
    public static final String CONTROLLED_SHUTDOWN_RETRY_BACKOFF_MS_DOC = "Before each retry, the system needs time to recover from the state that caused the previous failure (Controller fail over, replica lag etc). This config determines the amount of time to wait before retrying.";

// After
// (no additions)
```

**How to spot**
When reviewing server/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18417 remove controlled.shutdown.max.retries and controlled.shutdown.retry.backoff.ms (#18431).

**Test**
Test modified in UncleanLeaderElectionTest.scala covering the corrected behaviour.
