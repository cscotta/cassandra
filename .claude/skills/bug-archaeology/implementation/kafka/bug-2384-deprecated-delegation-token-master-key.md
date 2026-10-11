**Commit**: 02e3f7cc28 · **Ticket**: KAFKA-12601
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
  val delegationTokenSecretKey = Option(getPassword(DelegationTokenManagerConfigs.DELEGATION_TOKEN_SECRET_KEY_CONFIG))
    .getOrElse(getPassword(DelegationTokenManagerConfigs.DELEGATION_TOKEN_SECRET_KEY_ALIAS_CONFIG))
        case DelegationTokenManagerConfigs.DELEGATION_TOKEN_SECRET_KEY_ALIAS_CONFIG => // ignore
                therefore users should ensure that target broker is at least 2.3.0</li>
    @Deprecated
    public static final String DELEGATION_TOKEN_SECRET_KEY_ALIAS_CONFIG = "delegation.token.master.key";
    public static final String DELEGATION_TOKEN_SECRET_KEY_ALIAS_DOC = "DEPRECATED: An alias for " + DELEGATION_TOKEN_SECRET_KEY_CONFIG + ", which should be used instead of this config.";
            .define(DelegationTokenManagerConfigs.DELEGATION_TOKEN_SECRET_KEY_ALIAS_CONFIG, PASSWORD, null, MEDIUM, DelegationTokenManagerConfigs.DELEGATION_TOKEN_SECRET_KEY_ALIAS_DOC)

// After
  val delegationTokenSecretKey = getPassword(DelegationTokenManagerConfigs.DELEGATION_TOKEN_SECRET_KEY_CONFIG)
                therefore users should ensure that target broker is at least 2.3.0
            </li>
            <li> Remove <code>delegation.token.master.key</code>. please use <code>delegation.token.secret.key</code> instead of it.
            </li>
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-12601 remove deprecated `delegation.token.master.key` (#17082).

**Test**
Test modified in KafkaConfigTest.scala covering the corrected behaviour.
