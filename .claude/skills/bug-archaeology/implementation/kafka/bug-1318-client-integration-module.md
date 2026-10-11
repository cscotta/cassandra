**Commit**: 701573366f · **Ticket**: KAFKA-18933
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Adds a new ":clients:integration-test" Gradle module. Relocates one
example test from ":core"

Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    implementation project(':test-common:test-common-internal-api')
    implementation project(':clients')
    implementation project(':core')
    implementation project(':server')
    implementation project(':server-common')
package kafka.admin;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.FenceProducersOptions;
                ProducerConfig.TRANSACTION_TIMEOUT_CONFIG, "2000"));

// After
    api project(':core')
    api project(':clients')

    implementation project(':server')
    implementation project(':server-common')
    implementation project(':test-common:test-common-internal-api')
project(':clients:clients-integration-tests') {
  base {
    archivesName = "kafka-clients-integration-tests"
  }

  dependencies {
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18933 add client integration tests module (#19144).

**Test**
Test modified in import-control-clients-integration-tests.xml covering the corrected behaviour.
