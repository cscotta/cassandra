**Commit**: b8fc96272e · **Ticket**: KAFKA-18627
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Fix https://issues.apache.org/jira/browse/KAFKA-18627 and update same
test case


[KIP-link](https://cwiki.apache.org/confluence/pages/viewpage.action?pageId=340037077)

⚠️ This PR cannot be merged at the moment because KIPs for version 4.1
are already frozen. It will need to wait for version 4.2. update upgrade.html


![image](https://github.com/user-attachments/assets/00bd6579-6914-42a7-a1b8-4a0b0a8cc3c3)

add configuration doc

![image](https://github.com/user-attachments/assets/e20d5c97-29c6-4fe4-a41a-58267179e4e0)

Reviewers: TaiJuWu <tjwu1217@gmail.com>, Ken Huang <s7133700@gmail.com>,
 Luke Chen <showuon@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    private static void throwIfLoginModuleIsNotAllowed(AppConfigurationEntry appConfigurationEntry) {
        Set<String> disallowedLoginModuleList = Arrays.stream(
                System.getProperty(DISALLOWED_LOGIN_MODULES_CONFIG, DISALLOWED_LOGIN_MODULES_DEFAULT).split(","))
        String loginModuleName = appConfigurationEntry.getLoginModuleName().trim();
            throw new IllegalArgumentException(loginModuleName + " is not allowed. Update System property '"
                    + DISALLOWED_LOGIN_MODULES_CONFIG + "' to allow " + loginModuleName);

// After
import static org.apache.kafka.common.security.JaasUtils.ALLOWED_LOGIN_MODULES_CONFIG;
    @SuppressWarnings("deprecation")
    // Visible for testing
     static void throwIfLoginModuleIsNotAllowed(AppConfigurationEntry appConfigurationEntry) {
        String disallowedProperty = System.getProperty(DISALLOWED_LOGIN_MODULES_CONFIG);
        if (disallowedProperty != null) {
            LOG.warn("System property '{}' is deprecated and will be removed in a future release. Use '{}' instead.",
                    DISALLOWED_LOGIN_MODULES_CONFIG, ALLOWED_LOGIN_MODULES_CONFIG);
        }
        String loginModuleName = appConfigurationEntry.getLoginModuleName().trim();
        String allowedProperty = System.getProperty(ALLOWED_LOGIN_MODULES_CONFIG);
        if (allowedProperty != null) {
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18627 deprecated "org.apache.kafka.disallowed.login.modules" (#18683).

**Test**
Test modified in JaasContextTest.java covering the corrected behaviour.
