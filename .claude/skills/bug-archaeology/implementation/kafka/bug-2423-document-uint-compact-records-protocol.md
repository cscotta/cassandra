**Commit**: a9f3ea2dd3 · **Ticket**: KAFKA-17323
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
            UNSIGNED_INT32, VARINT, VARLONG, UUID, FLOAT64,
            RECORDS, new ArrayOf(STRING), new CompactArrayOf(COMPACT_STRING)};

// After
            UINT16, UNSIGNED_INT32, VARINT, VARLONG, UUID, FLOAT64,
            RECORDS, COMPACT_RECORDS, new ArrayOf(STRING), new CompactArrayOf(COMPACT_STRING)};
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17323 document uint16 and compact_records in protocol guide (#16868).

**Test**
No test added.
