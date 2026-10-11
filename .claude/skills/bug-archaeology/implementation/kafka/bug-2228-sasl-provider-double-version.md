**Commit**: 9b62c861fa · **Ticket**: KAFKA-17739
**Subsystem**: build (build.gradle, SASL providers)
**Category**: Wrong constant / default value

**Root cause**
The build's `minJavaVersion` was set to 8, causing Gradle to include Java 8 and 9 compatibility checks and keeping workarounds for API differences that no longer exist. Additionally, SASL security provider constructors used the deprecated `double version` overload (e.g., `new Provider("name", 1.0, "info")`), which is removed in JDK 23+. Bumping `minJavaVersion` to 11 removed the compat guards; the SASL providers were updated to `new Provider("name", "1.0", "info")`.

**Before/After**
```java
// Before – deprecated double version
Provider p = new SomeProvider("name", 1.0, "desc");

// After – String version (non-deprecated)
Provider p = new SomeProvider("name", "1.0", "desc");
```

**How to spot**
Search for `new Provider(` or `super(` in Provider subclasses; any call passing a primitive `double` as the second argument uses the deprecated overload that is removed in JDK 23; replace with the `String` form.

**Test**
No test added.
