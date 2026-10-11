**Commit**: 598eb13d07 · **Ticket**: KAFKA-15370
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
This patch adds ACL support for 2PC as a part of KIP-939

A new value will be added to the enum AclOperation: TWO_PHASE_COMMIT
((byte) 15 . When InitProducerId comes with enable2Pc=true, it would
have to have both WRITE and TWO_PHASE_COMMIT operation enabled on the
transactional id resource. The kafka-acls.sh tool is going to support a new --operation
TwoPhaseCommit. Reviewers: Artem Livshits <alivshits@confluent.io>, PoAn Yang
 <poan.yang@suse.com>, Justine Olshan <jolshan@confluent.io>

**Before/After**
```java
// Before
    DESCRIBE_TOKENS((byte) 14);
        new AclOperationTestInfo(AclOperation.DESCRIBE_TOKENS, 14, "describe_tokens", false)
      CLUSTER_ACTION, DESCRIBE_CONFIGS, ALTER_CONFIGS, IDEMPOTENT_WRITE, CREATE_TOKENS, DESCRIBE_TOKENS))
      CLUSTER_ACTION, DESCRIBE_CONFIGS, ALTER_CONFIGS, IDEMPOTENT_WRITE, CREATE_TOKENS, DESCRIBE_TOKENS))

// After
    DESCRIBE_TOKENS((byte) 14),

    /**
     * TWO_PHASE_COMMIT operation.
     */
    TWO_PHASE_COMMIT((byte) 15);
        new AclOperationTestInfo(AclOperation.DESCRIBE_TOKENS, 14, "describe_tokens", false),
        new AclOperationTestInfo(AclOperation.TWO_PHASE_COMMIT, 15, "two_phase_commit", false)
      if (initProducerIdRequest.enable2Pc() && !authHelper.authorize(request.context, TWO_PHASE_COMMIT, TRANSACTIONAL_ID, transactionalId)) {
        requestHelper.sendErrorResponseMaybeThrottle(request, Errors.TRANSACTIONAL_ID_AUTHORIZATION_FAILED.exception)
        return
      }
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15370: acl changes to support 2pc (kip-939) (#19364).

**Test**
Test modified in AclOperationTest.java covering the corrected behaviour.
