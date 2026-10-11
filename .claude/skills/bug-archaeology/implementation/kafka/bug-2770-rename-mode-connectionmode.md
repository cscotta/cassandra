**Commit**: 6897b06b03 · **Ticket**: KAFKA-3346
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        return create(securityProtocol, Mode.CLIENT, contextType, config, listenerName, false, clientSaslMechanism,
        return create(securityProtocol, Mode.SERVER, JaasContext.Type.SERVER, config, listenerName,
                                         Mode mode,
                requireNonNullMode(mode, securityProtocol);
                channelBuilder = new SslChannelBuilder(mode, listenerName, isInterBrokerListener, logContext);
                requireNonNullMode(mode, securityProtocol);
                if (mode == Mode.SERVER) {
                channelBuilder = new SaslChannelBuilder(mode,
    private static void requireNonNullMode(Mode mode, SecurityProtocol securityProtocol) {
        if (mode == null)
public enum Mode { CLIENT, SERVER }
    private final Mode mode;

// After
        return create(securityProtocol, ConnectionMode.CLIENT, contextType, config, listenerName, false, clientSaslMechanism,
        return create(securityProtocol, ConnectionMode.SERVER, JaasContext.Type.SERVER, config, listenerName,
                                         ConnectionMode connectionMode,
                requireNonNullMode(connectionMode, securityProtocol);
                channelBuilder = new SslChannelBuilder(connectionMode, listenerName, isInterBrokerListener, logContext);
                requireNonNullMode(connectionMode, securityProtocol);
                if (connectionMode == ConnectionMode.SERVER) {
                channelBuilder = new SaslChannelBuilder(connectionMode,
    private static void requireNonNullMode(ConnectionMode connectionMode, SecurityProtocol securityProtocol) {
        if (connectionMode == null)
public enum ConnectionMode { CLIENT, SERVER }
    private final ConnectionMode connectionMode;
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-3346 rename mode to connectionmode (#16403).

**Test**
Test modified in CertStores.java covering the corrected behaviour.
