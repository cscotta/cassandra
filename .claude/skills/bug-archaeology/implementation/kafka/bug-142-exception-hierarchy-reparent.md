**Commit**: 896f283e1e · **Ticket**: KAFKA-18783
**Subsystem**: clients/src/main/java/org/apache/kafka/common/errors/
**Category**: Missing override of default method

**Root cause**
A family of configuration- and validation-related exceptions (`AuthenticationException`, `AuthorizationException`, `InvalidTopicException`, `UnsupportedVersionException`, etc.) extended `ApiException` directly instead of the intermediate `InvalidConfigurationException`. This broke `catch (InvalidConfigurationException e)` handlers that were intended to catch all configuration-related errors, requiring callers to enumerate every concrete subclass individually. The fix re-parents each class to extend `InvalidConfigurationException` (which itself extends `ApiException`), preserving backward compatibility while enabling uniform catching.

**Before/After**
```java
// BEFORE
public class AuthenticationException extends ApiException { ... }
public class InvalidTopicException  extends ApiException { ... }

// AFTER
public class AuthenticationException extends InvalidConfigurationException { ... }
public class InvalidTopicException  extends InvalidConfigurationException { ... }
```

**How to spot**
When adding a new exception subtype to a hierarchy, check whether an intermediate class exists that groups related exceptions. If callers use `catch (SomeGroupException e)` patterns, all members of that group must inherit from the group class. Search for `extends ApiException` for exceptions whose names suggest a configuration or validation concern.

**Test**
`TransactionExceptionHierarchyTest.testInvalidConfigurationExceptionHierarchy`: parameterised test asserting that each of the 12 re-parented exception classes `isAssignableFrom(InvalidConfigurationException.class)`.
