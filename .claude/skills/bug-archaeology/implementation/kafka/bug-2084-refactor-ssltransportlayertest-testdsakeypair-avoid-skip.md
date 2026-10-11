**Commit**: 695f3aa03c · **Ticket**: KAFKA-17936
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import static org.junit.jupiter.api.Assumptions.assumeTrue;
    @ArgumentsSource(SslTransportLayerArgumentsProvider.class)
        assumeTrue(args.tlsProtocol.equals("TLSv1.2"));

// After
    private static class SslTransportLayerArgumentsForTLS2Provider extends SslTransportLayerArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
            return super.provideArguments(context).filter(arg -> ((Args) arg.get()[0]).tlsProtocol.equals("TLSv1.2"));
        }
    }

    @ArgumentsSource(SslTransportLayerArgumentsForTLS2Provider.class)
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17936 refactor ssltransportlayertest#testdsakeypair to avoid skip test (#17682).

**Test**
Test modified in SslTransportLayerTest.java covering the corrected behaviour.
