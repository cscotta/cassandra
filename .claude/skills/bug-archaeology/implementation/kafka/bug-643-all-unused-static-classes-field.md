**Commit**: 61e63f1263 · **Ticket**: KAFKA-19822
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Remove all unused static classes in `Field`. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    public static class Int8 extends Field {
        public Int8(String name, String docString) {
            super(name, Type.INT8, docString, false, null);
        }
        public Int8(String name, String docString, byte defaultValue) {
            super(name, Type.INT8, docString, true, defaultValue);
        }
    }

    public static class Int32 extends Field {
        public Int32(String name, String docString) {
            super(name, Type.INT32, docString, false, null);

// After
// (no additions)
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19822 remove all unused static classes in field (#20799).

**Test**
Test modified in StructTest.java covering the corrected behaviour.
