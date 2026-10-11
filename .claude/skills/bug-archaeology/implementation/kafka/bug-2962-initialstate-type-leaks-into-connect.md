**Commit**: 5cbc1d616a · **Ticket**: KAFKA-16881
**Subsystem**: connect/runtime
**Category**: Resource leak

**Root cause**
Reviewers: Chris Egerton <chrise@aiven.io>

**Before/After**
```java
// Before
    @JsonProperty

// After
    @JsonProperty("initial_state")
```

**How to spot**
When reviewing connect/runtime code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16881: initialstate type leaks into the connect rest api openapi spec (#16175).

**Test**
No test added.
