**Commit**: 4121a8952a · **Ticket**: KAFKA-17427
**Subsystem**: streams/src
**Category**: Resource leak

**Root cause**
Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After
    @Deprecated
    @Deprecated
    @Deprecated
    @Deprecated
    @Deprecated
    @Deprecated
    @Deprecated
    @Deprecated
    @Deprecated
    @Deprecated
```

**How to spot**
When reviewing streams/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17427: deprecate leaking *_doc variables in streamsconfig (#17171).

**Test**
No test added.
