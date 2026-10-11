**Commit**: 9a32a71e76 · **Ticket**: KAFKA-19699
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
document the behavior of "-1" (HIGH_WATERMARK)

Reviewers: Ken Huang <s7133700@gmail.com>, TengYao Chi
 <kitingiao@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
     * @param offset    the offset before which all records will be deleted
     * The offset before which all records will be deleted
          "about": "The deletion offset." }

// After
     * @param offset    The offset before which all records will be deleted.
     *                  Use {@code -1} to truncate to the high watermark.
     * The offset before which all records will be deleted.
     * Use {@code -1} to truncate to the high watermark.
          "about": "The deletion offset. -1 means that records should be truncated to the high watermark." }
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19699 improve the documentation of `recordstodelete` (#20527).

**Test**
No test added.
