**Commit**: a940d52070 · **Ticket**: KAFKA-18342
**Subsystem**: Vagrantfile
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
if File.exists?(local_config_file) then

// After
if File.exist?(local_config_file) then
```

**How to spot**
When reviewing Vagrantfile code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18342 use `file.exist` instead of `file.exists` to ensure the `vagrantfile` works with ruby 3.2+ (#18306).

**Test**
No test added.
