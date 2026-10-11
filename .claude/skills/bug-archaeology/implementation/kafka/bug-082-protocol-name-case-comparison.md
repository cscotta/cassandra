**Commit**: 7cd99ea66d · **Ticket**: KAFKA-19373
**Subsystem**: core/src/test/scala/integration/kafka/api/ (integration tests)
**Category**: Logic error in condition

**Root cause**: Parameterised integration tests receive the group protocol name as a lower-case string (e.g. `"classic"`) via JUnit's `@MethodSource`, while `GroupProtocol.CLASSIC.name` returns the enum constant name `"CLASSIC"`. Multiple tests compared the two with `equals()`, which is case-sensitive, so the branch that applied Classic-protocol-specific config (short session timeout, heartbeat interval, etc.) never executed for the Classic protocol run. The tests did not fail because the missing config caused them to be lenient, but Classic-specific behaviour was never actually exercised. All comparisons were changed to `equalsIgnoreCase`.

**Before/After**

```scala
// Before – always false when groupProtocol is lower-case "classic"
if (groupProtocol.equals(GroupProtocol.CLASSIC.name)) {
    this.consumerConfig.setProperty(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "5001")
    ...
}

// After
if (groupProtocol.equalsIgnoreCase(GroupProtocol.CLASSIC.name)) {
    ...
}
```

**How to spot**: Compare strings representing enum names only with `equalsIgnoreCase`, or normalise both sides to the same case before comparison. Parameterised test sources that produce lower-case strings are especially prone to this when matched against `Enum.name()` which is always upper-case.

**Test**: No new test added. The existing parameterised tests now correctly apply Classic-specific configuration, making them effective for that protocol.
