**Commit**: a842c02b88 · **Ticket**: KAFKA-18553
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Mickael Maison <mickael.maison@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>, Ismael Juma <ismael@juma.me.uk>, Andrew Schofield <aschofield@confluent.io>, Apoorv Mittal <amittal@confluent.io>

**Before/After**
```java
// Before
  private val BrokerSupportedConfigTypes = ConfigType.ALL.asScala :+ BrokerLoggerConfigType :+ ConfigType.CLIENT_METRICS :+ ConfigType.GROUP
import java.util.Arrays;
 * Represents all the entities that can be configured via ZK
    // Do not include ClientMetrics and Groups in `all` as they are not supported on ZK.
    public static final List<String> ALL = Arrays.asList(TOPIC, CLIENT, USER, BROKER, IP);

// After
  private val BrokerSupportedConfigTypes = ConfigType.ALL.asScala :+ BrokerLoggerConfigType
 * Represents all the entities that can be configured.
    public static final List<String> ALL = List.of(TOPIC, CLIENT, USER, BROKER, IP, CLIENT_METRICS, GROUP);
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18553: update javadoc and comments of configtype (#18567).

**Test**
No test added.
