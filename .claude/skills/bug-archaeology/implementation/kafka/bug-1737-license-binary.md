**Commit**: ce5bd197c1 · **Ticket**: KAFKA-18375
**Subsystem**: LICENSE-binary
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
error_prone_annotations-2.10.0
jakarta.validation-api-2.0.2
jetty-client-9.4.56.v20240826
jetty-http-9.4.56.v20240826
jetty-io-9.4.56.v20240826
jetty-security-9.4.56.v20240826
jetty-server-9.4.56.v20240826
jetty-util-9.4.56.v20240826
jakarta.activation-api-1.2.2
jakarta.xml.bind-api-2.3.3
jakarta.annotation-api-1.3.5
jakarta.ws.rs-api-2.1.6

// After
error_prone_annotations-2.14.0
jackson-jakarta-rs-base-2.16.2
jackson-jakarta-rs-json-provider-2.16.2
jackson-module-jakarta-xmlbind-annotations-2.16.2
jakarta.inject-api-2.0.1
jakarta.validation-api-3.0.2
jetty-alpn-client-12.0.15
jetty-client-12.0.15
jetty-ee10-servlet-12.0.15
jetty-ee10-servlets-12.0.15
jetty-http-12.0.15
jetty-io-12.0.15
```

**How to spot**
When reviewing LICENSE-binary code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18375 update the license-binary (#18359).

**Test**
No test added.
