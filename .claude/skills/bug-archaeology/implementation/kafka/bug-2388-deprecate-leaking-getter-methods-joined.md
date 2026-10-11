**Commit**: 9b53b525e8 · **Ticket**: KAFKA-17253
**Subsystem**: streams/src
**Category**: Resource leak

**Root cause**
Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After

    /**
     * @deprecated since 4.0 and should not be used any longer.
     */
    @Deprecated
    /**
     * @deprecated since 4.0 and should not be used any longer.
     */
    @Deprecated
    /**
     * @deprecated since 4.0 and should not be used any longer.
     */
```

**How to spot**
When reviewing streams/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17253: deprecate leaking getter methods in joined helper class (#17164).

**Test**
No test added.
