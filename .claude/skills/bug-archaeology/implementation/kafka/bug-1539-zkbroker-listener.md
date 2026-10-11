**Commit**: 0c9df75295 · **Ticket**: KAFKA-18474
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Ismael Juma <ismael@juma.me.uk>, Chia-Ping Tsai <chia7712@gmail.com>, PoAn Yang <payang@apache.org>

**Before/After**
```java
// Before
    public static EnumSet<ApiKeys> zkBrokerApis() {
        return apisForListener(ApiMessageType.ListenerType.ZK_BROKER);
    }

    public static EnumSet<ApiKeys> kraftBrokerApis() {
            .filter(apiKey -> apiKey.inScope(ApiMessageType.ListenerType.ZK_BROKER) || apiKey.inScope(ApiMessageType.ListenerType.BROKER))
  "listeners": ["zkBroker", "broker"],
  "listeners": ["zkBroker", "broker"],
  "listeners": ["zkBroker", "controller"],
  "listeners": ["zkBroker", "broker", "controller"],
  "listeners": ["zkBroker", "broker", "controller"],
  "listeners": ["broker", "controller", "zkBroker"],

// After
    public static EnumSet<ApiKeys> brokerApis() {
            .filter(apiKey -> apiKey.inScope(ApiMessageType.ListenerType.BROKER))
  "listeners": ["broker"],
  "listeners": ["broker"],
  "listeners": ["controller"],
  "listeners": ["broker", "controller"],
  "listeners": ["broker", "controller"],
  "listeners": ["broker", "controller"],
  "listeners": ["controller"],
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18474: remove zkbroker listener (#18477).

**Test**
Test modified in NetworkClientTest.java covering the corrected behaviour.
