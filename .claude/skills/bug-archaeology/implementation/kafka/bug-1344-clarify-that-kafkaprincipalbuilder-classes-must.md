**Commit**: 6ecf6817ad · **Ticket**: KAFKA-18919
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
In KRaft mode, custom KafkaPrincipalBuilder instances must implement KafkaPrincipalSerde. This PR updates all related documentation to highlight this requirement. Reviewers: Ken Huang <s7133700@gmail.com>, David Jacot <djacot@confluent.io>, TengYao Chi <kitingiao@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
            "and the SASL authentication ID for other mechanisms. For PLAINTEXT, the principal will be ANONYMOUS.";

// After
            "and the SASL authentication ID for other mechanisms. For PLAINTEXT, the principal will be ANONYMOUS. " +
            "Note that custom implementations of <code>KafkaPrincipalBuilder</code> is required to implement <code>KafkaPrincipalSerde</code> " +
            "interface, otherwise brokers will not be able to forward requests to the controller.";
 *
 * Note that custom implementations of {@link KafkaPrincipalBuilder}
 * must also implement {@link KafkaPrincipalSerde}, otherwise brokers will not be able to
 * forward requests to the controller.
        <li>
            <strong>Custom implementations of <code>KafkaPrincipalBuilder</code></strong>:
            In KRaft mode, custom implementations of <code>KafkaPrincipalBuilder</code> must also implement <code>KafkaPrincipalSerde</code>; otherwise brokers will not be able to
            forward requests to the controller.
        </li>
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18919 clarify that kafkaprincipalbuilder classes must also implement kafkaprincipalserde (#19104).

**Test**
No test added.
