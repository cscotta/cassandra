**Commit**: b2758f4ac6 · **Ticket**: KAFKA-16989
**Subsystem**: metadata/src
**Category**: Wrong serialization / deserialization

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        String value = "";
                    value += c;
                            entries.put(type, value);
                            value = "";
                            value += c;

// After
        StringBuilder value = new StringBuilder();
                    value.append(c);
                            entries.put(type, value.toString());
                            value = new StringBuilder();
                            value.append(c);
```

**How to spot**
When reviewing metadata/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16989 use stringbuilder instead of string concatenation (#16385).

**Test**
No test added.
