**Commit**: c23b6b0365 · **Ticket**: KAFKA-16327
**Subsystem**: streams/StreamsConfig
**Category**: Wrong constant / default value

**Root cause**
`StreamsConfig.TOPOLOGY_OPTIMIZATION` was a deprecated constant defined as `= TOPOLOGY_OPTIMIZATION_CONFIG`. Since both had identical string values, code using either constant compiled and ran correctly. The danger was that the deprecated constant could be referenced in documentation, examples, and user code, making it unclear which was the authoritative name. Removing `TOPOLOGY_OPTIMIZATION` enforces a single canonical name and prevents new code from accidentally using the deprecated alias.

**Before/After**
```java
// Before
@Deprecated
public static final String TOPOLOGY_OPTIMIZATION = TOPOLOGY_OPTIMIZATION_CONFIG;

// docs referenced TOPOLOGY_OPTIMIZATION

// After
// field removed; docs updated to reference TOPOLOGY_OPTIMIZATION_CONFIG
```

**How to spot**
When two public constants in the same class have identical string values and one is marked `@Deprecated`, reviewers should confirm both the constant removal and that all documentation, JavaDoc, and upgrade guides are updated to reference only the canonical name.

**Test**
No test added.
