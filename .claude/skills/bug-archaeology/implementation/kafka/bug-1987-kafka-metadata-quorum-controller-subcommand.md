**Commit**: eb641b5b1c · **Ticket**: KAFKA-18059
**Subsystem**: tools/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
            .help("Property file containing configs to be passed to Admin Client.");

// After
            .help("Property file containing configs to be passed to Admin Client. " +
                "For add-controller, the file is used to specify the controller properties as well.");
```

**How to spot**
When reviewing tools/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18059 kafka-metadata-quorum.sh add-controller subcommand can't recognize argument --config (#17907).

**Test**
No test added.
