**Commit**: f5fcc4188f · **Ticket**: KAFKA-19503

**Subsystem**: Core / MX4j HTTP monitoring

**Category**: Misleading/wrong documentation or log message

**Root cause**
MX4j-based HTTP monitoring support in Kafka was already effectively unused and unmaintained but had no deprecation notice. Without a formal deprecation warning, operators had no signal that the feature would be removed, and the upgrade guide did not mention it. The fix adds a `WARN`-level deprecation log at startup when MX4j is on the classpath, plus an entry in `docs/upgrade.html` so users are notified before the next major release removes it entirely.

**Before/After**
```java
// Before: MX4j configured silently with no deprecation signal
mx4jLoader.load();

// After: deprecation warning emitted before loading
log.warn("MX4j-based HTTP monitoring is deprecated and will be removed " +
         "in a future release. See KIP-XXXX for alternatives.");
mx4jLoader.load();
```

**How to spot**
Look for optional third-party integrations that are conditionally loaded at startup without any log statement indicating their lifecycle status. Features without deprecation warnings tend to be removed without adequate user notice.

**Test**
The upgrade documentation (`docs/upgrade.html`) is updated and the deprecation warning is confirmed to appear in broker logs during integration test startup when MX4j is present on the classpath.
