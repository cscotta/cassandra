**Commit**: 612e1299e4 · **Ticket**: KAFKA-18230
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
Reviewers: Mickael Maison <mickael.maison@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
                adminClientConfig.getLong(AdminClientConfig.METADATA_MAX_AGE_CONFIG), false);
                    MetadataResponse.AUTHORIZED_OPERATIONS_OMITTED)

// After
                handleNotControllerError(abstractResponse);
                handleNotControllerError(abstractResponse);
                handleNotControllerError(abstractResponse);
        // When sending requests directly to the follower controller, it might return NOT_LEADER_OR_FOLLOWER error.
        } else if (metadataManager.usingBootstrapControllers() && response.errorCounts().containsKey(Errors.NOT_LEADER_OR_FOLLOWER)) {
            handleNotControllerError(Errors.NOT_LEADER_OR_FOLLOWER);
                handleNotControllerError(response);
                handleNotControllerError(response);
                handleNotControllerError(response);
                adminClientConfig.getLong(AdminClientConfig.METADATA_MAX_AGE_CONFIG),
                config.containsKey(AdminClientConfig.BOOTSTRAP_CONTROLLERS_CONFIG));

```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18230: handle not controller or not leader error in admin client (#18165).

**Test**
Test modified in AdminClientUnitTestEnv.java covering the corrected behaviour.
