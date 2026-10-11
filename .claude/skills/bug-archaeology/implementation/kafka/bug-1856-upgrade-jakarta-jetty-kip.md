**Commit**: bd6d0fbf3d · **Ticket**: KAFKA-16437
**Subsystem**: LICENSE-binary
**Category**: Wrong serialization / deserialization

**Root cause**
This commit implements the changes for KIP-1032. This updates Kafka to Jakarta specs, JavaEE 10 and Jetty 12. The changes here primarily effect Kafka Connect and MM2. Todo/Notes:

1) I bumped the connect modules to JDK 17 but I also had to bump a couple other things that had a dependency on conect.

**Before/After**
```java
// Before
javax.ws.rs-api-2.1.1
    implementation libs.jacksonJaxrsJsonProvider
    testImplementation libs.jacksonJaxrsJsonProvider
    implementation libs.jacksonJaxrsJsonProvider
    implementation libs.jacksonJaxrsJsonProvider
    implementation libs.jettyServer
    implementation libs.jettyServlet
    implementation libs.jettyServlets
    implementation libs.jacksonJaxrsJsonProvider
    implementation(project(':connect:json')) {
      // this transitive dependency is not used in Streams, and it breaks SBT builds
      exclude module: 'javax.ws.rs-api'

// After
    implementation libs.jacksonJakartarsJsonProvider
    testImplementation libs.jacksonJakartarsJsonProvider
    implementation libs.jacksonJakartarsJsonProvider
    implementation libs.jacksonJakartarsJsonProvider
    implementation (libs.jettyServer) {
      exclude group: 'org.slf4j', module: 'slf4j-api'
    }
    implementation (libs.jettyServlet) {
      exclude group: 'org.slf4j', module: 'slf4j-api'
    }
    implementation (libs.jettyServlets) {
      exclude group: 'org.slf4j', module: 'slf4j-api'
```

**How to spot**
When reviewing LICENSE-binary code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16437 upgrade to jakarta and jetty 12 (kip-1032) (#16754).

**Test**
Test modified in BasicAuthSecurityRestExtensionTest.java covering the corrected behaviour.
