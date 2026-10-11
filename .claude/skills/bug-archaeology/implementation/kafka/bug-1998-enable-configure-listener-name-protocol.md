**Commit**: c6294aacef · **Ticket**: KAFKA-17721
**Subsystem**: test-common/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        // The brokerListenerName and brokerSecurityProtocol configurations must
            if (brokerSecurityProtocol != SecurityProtocol.PLAINTEXT) {
                brokerListenerName, brokerSecurityProtocol, new ListenerName("CONTROLLER"), SecurityProtocol.PLAINTEXT);
        ListenerName listenerName = ListenerName.normalised("FOOBAR");
                .setBrokerListenerName(listenerName)
        assertEquals(listenerName, testKitNodes.brokerListenerName());

// After
    public static final SecurityProtocol DEFAULT_CONTROLLER_SECURITY_PROTOCOL = SecurityProtocol.PLAINTEXT;
    public static final String DEFAULT_CONTROLLER_LISTENER_NAME = "CONTROLLER";
        // The broker and controller listener name and SecurityProtocol configurations must
        private ListenerName controllerListenerName = ListenerName.normalised(DEFAULT_CONTROLLER_LISTENER_NAME);
        private SecurityProtocol controllerSecurityProtocol = DEFAULT_CONTROLLER_SECURITY_PROTOCOL;
        public Builder setControllerListenerName(ListenerName listenerName) {
            this.controllerListenerName = listenerName;
            return this;
        }

        public Builder setControllerSecurityProtocol(SecurityProtocol securityProtocol) {
            this.controllerSecurityProtocol = securityProtocol;
```

**How to spot**
When reviewing test-common/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17721 enable to configure listener name and protocol for controller (#17525).

**Test**
Test modified in TestKitNodes.java covering the corrected behaviour.
