**Commit**: 8eacd81514 · **Ticket**: KAFKA-19698
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Update the order of assertEquals in the streams package of Kafka

Ticket : https://issues.apache.org/jira/browse/KAFKA-19698 Part of :
https://issues.apache.org/jira/browse/KAFKA-19097

Reviewers: Hong-Yi Chen <apalan60@gmail.com>, Chia-Ping Tsai
 <chia7712@gmail.com>

**Before/After**
```java
// Before
        assertEquals(e.getMessage(), "Invalid topology: Name \"" + invalidName +
            "\" is illegal, it contains a character other than " + "ASCII alphanumerics, '.', '_' and '-'");
        assertEquals(e.getMessage(), "supplier can't be null");
        assertEquals(e.getMessage(), "supplier can't be null");
        assertEquals(e.getMessage(), "store type can't be null");
        assertEquals(e.getMessage(), "supplier can't be null");
        assertEquals(e.getMessage(), "Retention must not be negative.");
        assertEquals(e.getMessage(), "Cannot set store type when store supplier is pre-configured.");
        assertEquals(e.getMessage(), "Invalid topology: Name is illegal, it can't be longer than " + maxNameLength +
                " characters, name: " + invalidStoreName);
        assertEquals(sysOut.toString(StandardCharsets.UTF_8), "[processor]: good, 2\n");
        assertEquals(sysOut.toString(StandardCharsets.UTF_8), "[label]: hello, 3\n");

// After
        assertEquals("Invalid topology: Name \"" + invalidName +
            "\" is illegal, it contains a character other than " + "ASCII alphanumerics, '.', '_' and '-'", e.getMessage());
        assertEquals("supplier can't be null", e.getMessage());
        assertEquals("supplier can't be null", e.getMessage());
        assertEquals("store type can't be null", e.getMessage());
        assertEquals("supplier can't be null", e.getMessage());
        assertEquals("Retention must not be negative.", e.getMessage());
        assertEquals("Cannot set store type when store supplier is pre-configured.", e.getMessage());
        assertEquals("Invalid topology: Name is illegal, it can't be longer than " + maxNameLength +
                " characters, name: " + invalidStoreName, e.getMessage());
        assertEquals("[processor]: good, 2\n", sysOut.toString(StandardCharsets.UTF_8));
        assertEquals("[label]: hello, 3\n", sysOut.toString(StandardCharsets.UTF_8));
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19698 streams package assertequals arguments adjustments  (#20573).

**Test**
Test modified in MaterializedTest.java covering the corrected behaviour.
