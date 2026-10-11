**Commit**: 26e45f62cd · **Ticket**: KAFKA-19894

**Subsystem**: core/src/test/scala/integration/kafka/security/SaslPlainSslEndToEndAuthorizationTest.scala

**Category**: State not cleaned up

**Root cause**
`SaslPlainSslEndToEndAuthorizationTest` was removed together with `AclAuthorizer` in a prior PR. However, the test class also served as the sole integration-level exercise of the `SASL/PLAIN` mechanism through its inheritance hierarchy, meaning `SASL/PLAIN` end-to-end authentication was no longer covered by any test after the deletion. The fix re-introduces the test class as a standalone `SASL/PLAIN` integration test (without ZK-ACL–specific logic) so the inheritance-based test coverage is restored.

**Before**
```scala
// Before: class deleted, SASL/PLAIN untested
// (no file)
```

**After**
```scala
// After: re-introduced without ZK-ACL tests
class SaslPlainSslEndToEndAuthorizationTest
    extends EndToEndAuthorizationTest { ... }
```

**How to spot**
After removing a test class that inherits from a base class containing shared tests, audit whether any mechanism or configuration previously covered only through that subclass is now untested; `SASL/PLAIN` was exactly this case.

**Test**
Run `SaslPlainSslEndToEndAuthorizationTest` and confirm all inherited end-to-end authorisation tests pass for the `SASL/PLAIN` mechanism.
