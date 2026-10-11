**Commit**: e90b246002 · **Ticket**: KAFKA-17555
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: TengYao Chi <kitingiao@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
            //assertDoesNotThrow(() -> testTrustManager.checkClientTrusted(validChainWithoutCa, "RSA"));
            //assertDoesNotThrow(() -> testTrustManager.checkClientTrusted(validChainWithoutCa, "RSA"));
            //assertDoesNotThrow(() -> testTrustManager.checkClientTrusted(validChainWithoutCa, "RSA"));

// After
            assertDoesNotThrow(() -> testTrustManager.checkClientTrusted(validChainWithoutCa, "RSA"));
            assertDoesNotThrow(() -> testTrustManager.checkClientTrusted(validChainWithoutCa, "RSA"));
            assertDoesNotThrow(() -> testTrustManager.checkClientTrusted(validChainWithoutCa, "RSA"));
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17555 uncomment all checks of testcommonnameloggingtrustmanagermixvalidandinvalidcertificates (#17220).

**Test**
Test modified in CommonNameLoggingTrustManagerFactoryWrapperTest.java covering the corrected behaviour.
