**Commit**: af03353f71 · **Ticket**: KAFKA-19659
**Subsystem**: clients/src/main/java/org/apache/kafka/clients/admin/UnregisterBrokerOptions.java
**Category**: Wrong serialization / deserialization

**Root cause**
`UnregisterBrokerOptions` extended `AbstractOptions<UpdateFeaturesOptions>` instead of `AbstractOptions<UnregisterBrokerOptions>`. The self-referential generic type is used so that fluent builder methods like `timeoutMs(int)` — defined in `AbstractOptions` — return the correct concrete type. With the wrong type parameter, `timeoutMs()` returned `UpdateFeaturesOptions`, making it impossible to call the method in a type-safe fluent chain without an unchecked cast. The test had worked around this by directly setting the raw field `options.timeoutMs = 10`, bypassing the type-safe API.

**Before/After**
```java
// Before: wrong type parameter — fluent methods return UpdateFeaturesOptions
public class UnregisterBrokerOptions extends AbstractOptions<UpdateFeaturesOptions> { }

// After: correct self-referential parameter
public class UnregisterBrokerOptions extends AbstractOptions<UnregisterBrokerOptions> { }
```

**How to spot**
All concrete `AbstractOptions` subclasses should use themselves as the type parameter (the CRTP / self-type pattern). Grep for `extends AbstractOptions<` and verify each class uses its own name as the type argument. The symptom is usually that the fluent builder method has the wrong return type and callers must either cast or use the raw field.

**Test**
The test for `KafkaAdminClient.unregisterBroker` was updated to use `new UnregisterBrokerOptions().timeoutMs(10)` instead of the raw field assignment, confirming the fluent API now compiles and returns the correct type.
