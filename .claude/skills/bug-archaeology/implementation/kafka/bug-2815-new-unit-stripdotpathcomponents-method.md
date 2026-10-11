**Commit**: 6aa8f3c119 · **Ticket**: KAFKA-16923
**Subsystem**: shell/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After

    @Test
    public void testStripDotPathComponents() {

        //double dots
        assertEquals(Arrays.asList("keep", "keep2"), CommandUtils.stripDotPathComponents(Arrays.asList("..", "keep", "keep2")));
        //single dots
        assertEquals(Arrays.asList("keep", "keep2"), CommandUtils.stripDotPathComponents(Arrays.asList(".", "keep", "keep2")));

        assertEquals(Arrays.asList(".keep", "keep2"), CommandUtils.stripDotPathComponents(Arrays.asList(".", ".keep", "keep2")));

        assertEquals(Arrays.asList(".keep", "keep2"), CommandUtils.stripDotPathComponents(Arrays.asList("..", ".keep", "keep2")));
```

**How to spot**
When reviewing shell/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16923 new unit test for stripdotpathcomponents method (#16259).

**Test**
Test modified in CommandUtilsTest.java covering the corrected behaviour.
