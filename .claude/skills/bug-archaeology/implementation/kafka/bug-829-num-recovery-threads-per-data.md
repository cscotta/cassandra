**Commit**: 21db7e00bc · **Ticket**: KAFKA-19573
**Subsystem**: config/broker.properties
**Category**: Logic error in condition

**Root cause**
The default value of `num.recovery.threads.per.data.dir` is now 2
according to KIP-1030. We should update config files which are still
setting 1. ---------

Signed-off-by: Federico Valeri <fedevaleri@gmail.com>

Reviewers: Luke Chen <showuon@gmail.com>

**Before/After**
```java
// Before
# This value is recommended to be increased for installations with data dirs located in RAID array.
num.recovery.threads.per.data.dir=1
# This value is recommended to be increased for installations with data dirs located in RAID array.
num.recovery.threads.per.data.dir=1
# This value is recommended to be increased for installations with data dirs located in RAID array.
num.recovery.threads.per.data.dir=1
# This value is recommended to be increased for installations with data dirs located in RAID array.
num.recovery.threads.per.data.dir=1

// After
# This value is recommended to be increased based on the installation resources.
num.recovery.threads.per.data.dir=2
# This value is recommended to be increased based on the installation resources.
num.recovery.threads.per.data.dir=2
# This value is recommended to be increased based on the installation resources.
num.recovery.threads.per.data.dir=2
# This value is recommended to be increased based on the installation resources.
num.recovery.threads.per.data.dir=2
```

**How to spot**
When reviewing config/broker.properties code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19573: update num.recovery.threads.per.data.dir configs (#20299).

**Test**
No test added.
