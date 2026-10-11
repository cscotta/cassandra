**Commit**: c20c244fd5 · **Ticket**: KAFKA-17810
**Subsystem**: LICENSE-binary
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
jetty-client-9.4.54.v20240208
jetty-continuation-9.4.54.v20240208
jetty-http-9.4.54.v20240208
jetty-io-9.4.54.v20240208
jetty-security-9.4.54.v20240208
jetty-server-9.4.54.v20240208
jetty-servlet-9.4.54.v20240208
jetty-servlets-9.4.54.v20240208
jetty-util-9.4.54.v20240208
jetty-util-ajax-9.4.54.v20240208
  jetty: "9.4.54.v20240208",

// After
jetty-client-9.4.56.v20240826
jetty-continuation-9.4.56.v20240826
jetty-http-9.4.56.v20240826
jetty-io-9.4.56.v20240826
jetty-security-9.4.56.v20240826
jetty-server-9.4.56.v20240826
jetty-servlet-9.4.56.v20240826
jetty-servlets-9.4.56.v20240826
jetty-util-9.4.56.v20240826
jetty-util-ajax-9.4.56.v20240826
  jetty: "9.4.56.v20240826",
```

**How to spot**
When reviewing LICENSE-binary code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17810 upgrade jetty because of cve-2024-8184 (#17517).

**Test**
No test added.
