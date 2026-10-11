**Commit**: f369771bf2 · **Ticket**: KAFKA-16851
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Add a remote.log.disable.policy on a topic-level only as part of KIP-950

Reviewers: Kamal Chandraprakash <kchandraprakash@uber.com>, Luke Chen <showuon@gmail.com>, Murali Basani <muralidhar.basani@aiven.io>

**Before/After**
```java
// Before
import org.junit.jupiter.params.provider.ValueSource

// After
    public static final String REMOTE_LOG_DISABLE_POLICY_RETAIN = "retain";
    public static final String REMOTE_LOG_DISABLE_POLICY_DELETE = "delete";

    public static final String REMOTE_LOG_DISABLE_POLICY_CONFIG = "remote.log.disable.policy";

    public static final String REMOTE_LOG_DISABLE_POLICY_DOC = String.format("Determines whether tiered data for a topic should be retained or " +
            "deleted after tiered storage disablement on a topic. The two valid options are \"%s\" and \"%s\". If %s is " +
            "selected then all data in remote will be kept post-disablement and will only be deleted when it breaches expiration " +
            "thresholds. If %s is selected then the data will be made inaccessible immediately by advancing the log start offset and will be " +
            "deleted asynchronously.", REMOTE_LOG_DISABLE_POLICY_RETAIN, REMOTE_LOG_DISABLE_POLICY_DELETE,
            REMOTE_LOG_DISABLE_POLICY_RETAIN, REMOTE_LOG_DISABLE_POLICY_DELETE);

```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16851: add remote.log.disable.policy (#16132).

**Test**
Test modified in RemoteTopicCrudTest.scala covering the corrected behaviour.
