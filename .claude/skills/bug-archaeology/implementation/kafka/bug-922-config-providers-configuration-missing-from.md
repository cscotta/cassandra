**Commit**: b919836551 · **Ticket**: KAFKA-17662
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Ensure the config.providers configuration is documented for all
components supporting it

Reviewers: Mickael Maison <mickael.maison@gmail.com>, Greg Harris
<gharris1727@gmail.com>, Matthias J. Sax <mjsax@apache.org>

**Before/After**
```java
// Before
                                        METADATA_RECOVERY_REBOOTSTRAP_TRIGGER_MS_DOC);
                                        ConsumerConfig.SHARE_ACKNOWLEDGEMENT_MODE_DOC);
                                        CommonClientConfigs.METADATA_RECOVERY_REBOOTSTRAP_TRIGGER_MS_DOC);

// After
import java.util.List;
                                        METADATA_RECOVERY_REBOOTSTRAP_TRIGGER_MS_DOC)
                                .define(CONFIG_PROVIDERS_CONFIG,
                                        ConfigDef.Type.LIST,
                                        List.of(),
                                        ConfigDef.Importance.LOW,
                                        CONFIG_PROVIDERS_DOC);
                                        ConsumerConfig.SHARE_ACKNOWLEDGEMENT_MODE_DOC)
                                .define(CONFIG_PROVIDERS_CONFIG,
                                        ConfigDef.Type.LIST,
                                        List.of(),
                                        ConfigDef.Importance.LOW,
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17662: config.providers configuration missing from the docs (#18930).

**Test**
Test modified in KafkaConfigTest.scala covering the corrected behaviour.
