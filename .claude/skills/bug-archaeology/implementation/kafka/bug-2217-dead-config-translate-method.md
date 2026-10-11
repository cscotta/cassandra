**Commit**: b3b86d52f4 · **Ticket**: KAFKA-17717
**Subsystem**: common (ConfigUtils)
**Category**: State not cleaned up

**Root cause**
`ConfigUtils.translateDeprecatedConfigs` was a method that had been entirely superseded by the config framework's built-in deprecation mechanism. No production code called it; it existed only as test infrastructure that tested itself. Leaving it in place risked future callers using the legacy path and bypassing the framework, and it added noise to the public API surface.

**Before/After**
```java
// Before – dead method present
public static Map<String, Object> translateDeprecatedConfigs(
    Map<String, ?> configs, Map<String, String[]> aliasGroups) { ... }

// After – method removed entirely
```

**How to spot**
Use IDE or `grep` to find methods with zero callers outside their own test class; methods that only appear in a `*Test.java` that tests the method itself are dead code and safe to remove if the corresponding functionality is covered by the framework.

**Test**
No test added (test class removed along with method).
