**Commit**: 7ca02fd908 · **Ticket**: KAFKA-16617
**Subsystem**: server/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    public static final String ADVERTISED_LISTENERS_DOC = String.format(
            "Listeners to publish to ZooKeeper for clients to use, if different than the <code>%s</code> config property." +

// After
    public static final String ADVERTISED_LISTENERS_DOC = String.format("Specifies the listener addresses that the Kafka brokers will advertise to clients and other brokers." +
                    " The config is useful where the actual listener configuration <code>%s</code> does not represent the addresses that clients should" +
                    " use to connect, such as in cloud environments. In environments using ZooKeeper, these addresses are published to ZooKeeper." +
                    " In Kraft mode, the address would be published to and managed by kraft controller, the brokers would pull these data from controller as needed." +
```

**How to spot**
When reviewing server/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16617 add kraft info for the `advertised.listeners` doc description (#17552).

**Test**
No test added.
