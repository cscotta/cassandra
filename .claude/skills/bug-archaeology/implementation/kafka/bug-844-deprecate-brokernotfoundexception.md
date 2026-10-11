**Commit**: de2adb69de · **Ticket**: KAFKA-12281
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Implements KIP-1195. BrokerNotFoundException exception is unused since 2.8  Marking it
deprecated so that it can be removed in next major release. Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After
 * @deprecated since 4.2 and should not be used any longer.
@Deprecated
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-12281: deprecate brokernotfoundexception (#20192).

**Test**
No test added.
