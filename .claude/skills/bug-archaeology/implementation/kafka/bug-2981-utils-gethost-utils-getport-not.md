**Commit**: 0971924ebc · **Ticket**: KAFKA-16824
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Modify regex of HOST_PORT_PATTERN to prevent malformed hosts and ports. Reviewers: Luke Chen <showuon@gmail.com>

**Before/After**
```java
// Before
    // This matches URIs of formats: host:port and protocol:\\host:port
    private static final Pattern HOST_PORT_PATTERN = Pattern.compile(".*?\\[?([0-9a-zA-Z\\-%._:]*)\\]?:([0-9]+)");

// After
    // This matches URIs of formats: host:port and protocol://host:port
    private static final Pattern HOST_PORT_PATTERN = Pattern.compile("^(?:[a-zA-Z][a-zA-Z\\d+-.]*://)?\\[?([0-9a-zA-Z\\-._%:]+)\\]?:([0-9]+)$");
        // valid

        // invalid
        assertNull(getHost("PLAINTEXT://mydo)main.com:8080"));
        assertNull(getHost("PLAINTEXT://mydo(main.com:8080"));
        assertNull(getHost("PLAINTEXT://mydo()main.com:8080"));
        assertNull(getHost("PLAINTEXT://mydo(main).com:8080"));
        assertNull(getHost("ho)st:9092"));
        assertNull(getHost("ho(st:9092"));
        assertNull(getHost("ho()st:9092"));
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16824: utils.gethost and utils.getport do not catch a lot of invalid host and ports. (#16048).

**Test**
Test modified in UtilsTest.java covering the corrected behaviour.
