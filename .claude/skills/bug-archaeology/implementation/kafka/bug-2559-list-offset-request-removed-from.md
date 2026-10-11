**Commit**: b767c65527 · **Ticket**: KAFKA-17326
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
            .filter(apiKey -> !apiKey.messageType.latestVersionUnstable())
            if (apiKeys.messageType.latestVersionUnstable()) {
                assertFalse(html.contains("The_Messages_" + apiKeys.name), "Html should not contain unstable api: " + apiKeys.name);
            } else {

// After
            .filter(apiKey -> apiKey.toApiVersion(false).isPresent())
            if (apiKeys.toApiVersion(false).isPresent()) {
            } else {
                assertFalse(html.contains("The_Messages_" + apiKeys.name), "Html should not contain unstable api: " + apiKeys.name);
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17326 the list_offset request is removed from the "api keys" page (#16870).

**Test**
Test modified in ApiKeysTest.java covering the corrected behaviour.
