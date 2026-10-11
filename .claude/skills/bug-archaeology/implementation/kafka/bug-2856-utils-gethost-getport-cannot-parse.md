**Commit**: 09bc5be63e · **Ticket**: KAFKA-16946
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
In previous PR(#16048), I mistakenly excluded the underscore (_) from the set of valid characters for the protocol,
resulting in the inability to correctly parse the connection string for SASL_PLAINTEXT. This bug fix addresses the
issue and includes corresponding tests. Reviewers: Andrew Schofield <aschofield@confluent.io>, Luke Chen <showuon@gmail.com>

**Before/After**
```java
// Before
    private static final Pattern HOST_PORT_PATTERN = Pattern.compile("^(?:[a-zA-Z][a-zA-Z\\d+-.]*://)?\\[?([0-9a-zA-Z\\-._%:]+)\\]?:([0-9]+)$");
    @Test
    public void testGetHost() {
        // valid
        assertEquals("mydomain.com", getHost("PLAINTEXT://mydomain.com:8080"));
        assertEquals("MyDomain.com", getHost("PLAINTEXT://MyDomain.com:8080"));
        assertEquals("My_Domain.com", getHost("PLAINTEXT://My_Domain.com:8080"));
        assertEquals("2001:db8:85a3:8d3:1319:8a2e:370:7348", getHost("PLAINTEXT://[2001:db8:85a3:8d3:1319:8a2e:370:7348]:5678"));
        assertEquals("2001:DB8:85A3:8D3:1319:8A2E:370:7348", getHost("PLAINTEXT://[2001:DB8:85A3:8D3:1319:8A2E:370:7348]:5678"));
        assertEquals("fe80::b1da:69ca:57f7:63d8%3", getHost("PLAINTEXT://[fe80::b1da:69ca:57f7:63d8%3]:5678"));
        // invalid
        assertNull(getHost("PLAINTEXT://mydo)main.com:8080"));

// After
    private static final Pattern HOST_PORT_PATTERN = Pattern.compile("^(?:[0-9a-zA-Z\\-%._]*://)?\\[?([0-9a-zA-Z\\-%._:]*)]?:([0-9]+)");
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
    @ParameterizedTest
    @CsvSource(value = {"PLAINTEXT", "SASL_PLAINTEXT", "SSL", "SASL_SSL"})
    public void testGetHostValid(String protocol) {
        assertEquals("mydomain.com", getHost(protocol + "://mydomain.com:8080"));
        assertEquals("MyDomain.com", getHost(protocol + "://MyDomain.com:8080"));
        assertEquals("My_Domain.com", getHost(protocol + "://My_Domain.com:8080"));
        assertEquals("::1", getHost(protocol + "://[::1]:1234"));
        assertEquals("2001:db8:85a3:8d3:1319:8a2e:370:7348", getHost(protocol + "://[2001:db8:85a3:8d3:1319:8a2e:370:7348]:5678"));
        assertEquals("2001:DB8:85A3:8D3:1319:8A2E:370:7348", getHost(protocol + "://[2001:DB8:85A3:8D3:1319:8A2E:370:7348]:5678"));
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16946: utils.gethost/getport cannot parse sasl_plaintext://host:port (#16319).

**Test**
Test modified in UtilsTest.java covering the corrected behaviour.
