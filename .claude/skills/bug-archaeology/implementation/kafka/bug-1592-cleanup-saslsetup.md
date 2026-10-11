**Commit**: 029d9184c6 · **Ticket**: KAFKA-18565
**Subsystem**: core/src
**Category**: State not cleaned up

**Root cause**
Reviewers: Christo Lolov <lolovc@amazon.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    startSasl(jaasSections(kafkaServerSaslMechanisms, Some("SCRAM-SHA-256"), KafkaSasl, JaasTestUtils.KAFKA_SERVER_CONTEXT_NAME))
      startSasl(jaasSections(kafkaServerSaslMechanisms, Option(kafkaClientSaslMechanism), KafkaSasl))
    startSasl(jaasSections(Seq("GSSAPI"), Some("GSSAPI"), Both, JaasTestUtils.KAFKA_SERVER_CONTEXT_NAME))
    startSasl(jaasSections(kafkaServerSaslMechanisms, Some(kafkaClientSaslMechanism), KafkaSasl, kafkaServerJaasEntryName))
    startSasl(jaasSections(List.empty, None, KafkaSasl))
    startSasl(jaasSections(kafkaServerSaslMechanisms, Some(kafkaClientSaslMechanism), Both,

// After
    startSasl(jaasSections(kafkaServerSaslMechanisms, Some("SCRAM-SHA-256"), JaasTestUtils.KAFKA_SERVER_CONTEXT_NAME))
      startSasl(jaasSections(kafkaServerSaslMechanisms, Option(kafkaClientSaslMechanism)))
    startSasl(jaasSections(Seq("GSSAPI"), Some("GSSAPI"), JaasTestUtils.KAFKA_SERVER_CONTEXT_NAME))
    startSasl(jaasSections(kafkaServerSaslMechanisms, Some(kafkaClientSaslMechanism), kafkaServerJaasEntryName))
    startSasl(jaasSections(List.empty, None))
    startSasl(jaasSections(kafkaServerSaslMechanisms, Some(kafkaClientSaslMechanism),
```

**How to spot**
When reviewing core/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18565 cleanup saslsetup (#18586).

**Test**
Test modified in CustomQuotaCallbackTest.scala covering the corrected behaviour.
