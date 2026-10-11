**Commit**: e8e4e0b6db · **Ticket**: KAFKA-19377

**Subsystem**: Kafka Streams / Documentation (ACL / Security)

**Category**: Misleading/wrong documentation or log message

**Root cause**
KIP-1071 introduced two new Kafka Streams APIs (`STREAMS_GROUP_HEARTBEAT` and `STREAMS_GROUP_DESCRIBE`) that require explicit ACL grants. The Streams security documentation did not list these new API operations among the required ACLs, leaving operators with incomplete ACL configurations that would cause `AuthorizationException` at runtime without a clear explanation.

**Before/After**
```
// Before: security docs list only legacy group/consumer ACLs
// STREAMS_GROUP_HEARTBEAT and STREAMS_GROUP_DESCRIBE not mentioned

// After: both new operations documented with required ACL entries
* Group ID: <application-id>
  - STREAMS_GROUP_HEARTBEAT: Allow
  - STREAMS_GROUP_DESCRIBE: Allow
```

**How to spot**
After every KIP that introduces new request types used by Streams, cross-check the Streams security documentation to ensure the new API operations appear in the required-ACLs table; missing entries lead to runtime authorization failures that are hard to diagnose.

**Test**
Documentation review confirms that `STREAMS_GROUP_HEARTBEAT` and `STREAMS_GROUP_DESCRIBE` now appear in the required ACL list in the Kafka Streams security documentation page.
