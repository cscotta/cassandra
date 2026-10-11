**Commit**: 6b5d172dcf · **Ticket**: KAFKA-12828
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
    /**
     * Get the active Kafka Streams instance for given key.
     *
     * @return active instance's {@link HostInfo}
     * @deprecated since 2.7.0; use {@link #activeHost()} instead.
     */
    @Deprecated
    public HostInfo getActiveHost() {
        return activeHost;
    }

    /**

// After
// (no additions)
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-12828: removed deprecated methods under keyquerymetadata (#16747).

**Test**
No test added.
