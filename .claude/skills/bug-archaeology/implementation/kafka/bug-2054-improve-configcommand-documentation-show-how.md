**Commit**: 7e5ffd3403 · **Ticket**: KAFKA-17583
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Andrew Schofield <aschofield@confluent.io>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
      require(configsToBeAdded.forall(config => config.length == 2), "Invalid entity config: all configs to be added must be in the format \"key=val\".")

// After
      require(configsToBeAdded.forall(config => config.length == 2), "Invalid entity config: all configs to be added must be in the format \"key=val\" or  \"key=[val1,val2]\" to group values which contain commas.")
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17583 improve the configcommand documentation to show how to set `key=[val1,val2]` (#17243).

**Test**
No test added.
