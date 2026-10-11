**Commit**: 2346c0e737 · **Ticket**: KAFKA-19495

**Subsystem**: Native image / GraalVM configuration

**Category**: Missing configuration entry

**Root cause**
GraalVM native-image compilation requires all classes accessed via reflection to be listed in `reflect-config.json`. Four classes — `DefaultJwtRetriever`, `DefaultJwtValidator`, `LoggingController`, and `DefaultStatePersister` — were missing from this list. At runtime the native image would throw `ClassNotFoundException` or similar reflective-access failures when these classes were instantiated via reflection (e.g., from configuration-driven class loading), causing authentication, logging, or state-persistence initialization to fail silently or with cryptic errors.

**Before/After**
```json
// Before: entries absent from reflect-config.json

// After: entries added
{ "name": "org.apache.kafka.common.security.oauthbearer.internals.unsecured.OAuthBearerUnsecuredJwtRetriever", ... },
{ "name": "org.apache.kafka.common.security.oauthbearer.internals.unsecured.OAuthBearerUnsecuredJwtValidator", ... },
{ "name": "kafka.server.LoggingController", ... },
{ "name": "org.apache.kafka.streams.state.internals.DefaultStatePersister", ... }
```

**How to spot**
After adding a new class that is instantiated by name (via `Class.forName`, plugin loading, or `KafkaConfig`), add it to `reflect-config.json`. Run the native-image agent (`-agentlib:native-image-agent`) against the full test suite to capture any missing entries automatically.

**Test**
Native image build and smoke tests confirm that authentication, logging, and state persistence initialize correctly without reflection errors after the entries are added.
