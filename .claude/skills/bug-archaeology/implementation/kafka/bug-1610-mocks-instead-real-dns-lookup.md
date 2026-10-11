**Commit**: 5c20aa187a · **Ticket**: KAFKA-18546
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Since the example.com DNS lookup changed the second time within one
year, we rewrote the unit tests for ClientUtils so that they do
not make a real DNS lookup to the outside but use mocks. Reviewers: PoAn Yang <payang@apache.org>, Chia-Ping Tsai <chia7712@gmail.com>, Lianet Magrans <lmagrans@confluent.io>

**Before/After**
```java
// Before
import java.util.stream.Collectors;
    private final HostResolver hostResolver = new DefaultHostResolver();

        // With lookup of example.com, either one or two addresses are expected depending on
        // whether ipv4 and ipv6 are enabled
        List<InetSocketAddress> validatedAddresses = checkWithLookup(Collections.singletonList("example.com:10000"));
        assertFalse(validatedAddresses.isEmpty(), "Unexpected addresses " + validatedAddresses);
        List<String> validatedHostNames = validatedAddresses.stream().map(InetSocketAddress::getHostName)
                .collect(Collectors.toList());
        List<String> expectedHostNames = List.of(
            "a23-215-0-136.deploy.static.akamaitechnologies.com",
            "a23-192-228-84.deploy.static.akamaitechnologies.com",

// After
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
        String hostname = "example.com";
        Integer port = 10000;
        String canonicalHostname1 = "canonical_hostname1";
        String canonicalHostname2 = "canonical_hostname2";
        try (final MockedStatic<InetAddress> inetAddress = mockStatic(InetAddress.class)) {
            InetAddress inetAddress1 = mock(InetAddress.class);
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18546: use mocks instead of a real dns lookup to the outside (#18565).

**Test**
Test modified in ClientUtilsTest.java covering the corrected behaviour.
