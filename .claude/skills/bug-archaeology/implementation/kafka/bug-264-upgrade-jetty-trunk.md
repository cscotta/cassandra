**Commit**: 55a7c2fa74 · **Ticket**: KAFKA-20344
**Subsystem**: LICENSE-binary
**Category**: Logic error in condition

**Root cause**
Upgrade Jetty from 12.0.25 to 12.0.34. Jetty 12.0.34 has removed all dependencies on the SLF4J 2.x API,
resolving the previous incompatibility with Kafka's SLF4J 1.7.x usage. This also fixes CVE-2025-11143 in jetty-http. This can be cherry-picked to 4.3 and 4.2.

**Before/After**
```java
// Before
- jetty-alpn-client-12.0.25
- jetty-client-12.0.25
- jetty-ee10-servlet-12.0.25
- jetty-ee10-servlets-12.0.25
- jetty-http-12.0.25
- jetty-io-12.0.25
- jetty-security-12.0.25
- jetty-server-12.0.25
- jetty-session-12.0.25
- jetty-util-12.0.25
  // Jetty 12.0.30+ introduced SLF4J 2.x fluent API usage (e.g. Logger.atDebug()) in production
  // code, which causes NoSuchMethodError at runtime since Kafka uses SLF4J 1.7.x.

// After
- jetty-alpn-client-12.0.34
- jetty-client-12.0.34
- jetty-ee10-servlet-12.0.34
- jetty-ee10-servlets-12.0.34
- jetty-http-12.0.34
- jetty-io-12.0.34
- jetty-security-12.0.34
- jetty-server-12.0.34
- jetty-session-12.0.34
- jetty-util-12.0.34
  // When upgrading Jetty, verify that it does not use the SLF4J 2.x fluent API
  // (e.g. Logger.atDebug()) in production code, as Kafka uses SLF4J 1.7.x.
```

**How to spot**
When reviewing LICENSE-binary code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20344: upgrade jetty to 12.0.34 (trunk, 4.3, 4.2) (#21939).

**Test**
No test added.
