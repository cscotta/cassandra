**Commit**: 93499df6e9 · **Ticket**: KAFKA-18924
**Subsystem**: storage/src
**Category**: Logic error in condition

**Root cause**
Change log4j2 logging directory to `build/kafka-storage-test/`

Reviewers: Sanskar Jhajharia <sjhajharia@confluent.io>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        fileName: storage.log
        filePattern: "storage-%d{yyyy-MM-dd}.log"

// After
        fileName: build/kafka-storage-test/storage.log
        filePattern: "build/kafka-storage-test/storage-%d{yyyy-MM-dd}.log"
```

**How to spot**
When reviewing storage/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18924 running the storage module tests produces a storage/storage.log file (#19147).

**Test**
Test modified in log4j2.yaml covering the corrected behaviour.
