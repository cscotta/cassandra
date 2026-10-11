**Commit**: 9b29e91218 · **Ticket**: KAFKA-18791
**Subsystem**: .asf.yaml
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
# Enable the next-gen .asf.yaml parser
meta:
  nextgen: true

```

**How to spot**
When reviewing .asf.yaml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18791 enable new asf.yaml parser [1/n] (#18955).

**Test**
No test added.
