**Commit**: 63304fb6e5 · **Ticket**: KAFKA-17028
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
KIP-932 introduces FindCoordinator v6 for finding share coordinators. The initial implementation:

Checks that share coordinators are only requested with v6 or above. Share coordinator requests are authorized as cluster actions (this is for inter-broker use only)
Responds with COORDINATOR_NOT_AVAILABLE because share coordinators are not yet available. When the share coordinator code is delivered, the request handling will be gated by configurations which enable share groups and the share coordinator specifically.

**Before/After**
```java
// Before

            Utils.mkSet(GROUP_METADATA_TOPIC_NAME, TRANSACTION_STATE_TOPIC_NAME));
        GROUP((byte) 0), TRANSACTION((byte) 1);

// After
                        break;
                    default:
                        log.error("Group coordinator lookup failed: Unexpected coordinator type in response");
                        fatalError(new IllegalStateException("Group coordinator lookup failed: Unexpected coordinator type in response"));
    public static final String SHARE_GROUP_STATE_TOPIC_NAME = "__share_group_state";
            Utils.mkSet(GROUP_METADATA_TOPIC_NAME, TRANSACTION_STATE_TOPIC_NAME, SHARE_GROUP_STATE_TOPIC_NAME));
        GROUP((byte) 0), TRANSACTION((byte) 1), SHARE((byte) 2);
                case 2:
                    return SHARE;
    else if (keyType == CoordinatorType.SHARE.id && request.context.apiVersion < 6)
      (Errors.INVALID_REQUEST, Node.noNode)
    else if (keyType == CoordinatorType.SHARE.id &&
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17028: findcoordinator v6 initial implementation (#16440).

**Test**
Test modified in KafkaApisTest.scala covering the corrected behaviour.
