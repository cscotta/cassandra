**Commit**: 98252cae14 · **Ticket**: KAFKA-16681
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
    testImplementation libs.apachedsCoreApi
    testImplementation libs.apachedsInterceptorKerberos
    testImplementation libs.apachedsProtocolShared
    testImplementation libs.apachedsProtocolKerberos
    testImplementation libs.apachedsProtocolLdap
    testImplementation libs.apachedsLdifPartition

  <subpackage name="security">
    <allow pkg="org.apache.commons" />
    <allow pkg="org.apache.directory" />
    <allow pkg="org.apache.mina.core.service" />
  </subpackage>
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16681 rewrite minikdc by java (#16213).

**Test**
Test modified in MiniKdc.java covering the corrected behaviour.
