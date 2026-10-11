**Commit**: 2f6ea81d0a · **Ticket**: KAFKA-19488

**Subsystem**: Tools / CLI documentation

**Category**: Misleading/wrong documentation or log message

**Root cause**
The `--if-not-exists` option for topic creation was documented as "action will only execute if topic does not exist." In reality, the create request is always sent to the broker; the flag merely suppresses the `TopicExistsException` error on the client side. The misleading documentation implied a client-side pre-check that does not happen, causing operators to rely on it for idempotency guarantees it does not provide.

**Before/After**
```
// Before: implies conditional execution
--if-not-exists  Action will only execute if topic does not exist.

// After: accurately describes error suppression
--if-not-exists  If set, the create request is still sent, but
                 TopicExistsException errors are suppressed.
```

**How to spot**
Option documentation that says "action will only execute if X" when the actual behavior is "action always executes but errors are suppressed when X" is a common source of operator confusion. Review CLI help text for conditional-sounding descriptions that hide always-execute semantics.

**Test**
CLI `--help` output test confirms the corrected description for `--if-not-exists`.
