**Commit**: d830179375 · **Ticket**: KAFKA-18675
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Ken Huang <s7133700@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
    @Test
    public void testValidBrokerAddress() {
        List<String> validBrokerAddress = List.of("localhost:9997", "localhost:9998", "localhost:9999");
        assertDoesNotThrow(() -> ClientUtils.parseAndValidateAddresses(validBrokerAddress, ClientDnsLookup.USE_ALL_DNS_IPS));
    }

    static Stream<List<String>> provideInvalidBrokerAddressTestCases() {
        return Stream.of(
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18675 add tests for valid and invalid broker addresses (#18781).

**Test**
Test modified in ClientUtilsTest.java covering the corrected behaviour.
