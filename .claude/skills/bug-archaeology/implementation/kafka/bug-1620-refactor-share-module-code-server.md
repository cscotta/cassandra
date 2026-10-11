**Commit**: a1f7457389 · **Ticket**: KAFKA-18514
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
As per the discussion with @ijuma and @mumrah, the `share` module seems not required and it's advised to user `server` and `server-common` instead. The PR moves the classes from `share` module to respective server related modules. Following has been refactored in the PR:

- Moved Share Fetch, Acknowledge, Session, Context and Cache related classes to `server` module as the classes are used by `core` and `tools` modules. - Moved `Persister` releated classes from `share` to `server-common` as the Persister classes though currently just being used by `core` module but in [near future](https://github.com/apache/kafka/pull/17775) will also be used by `group-coordinator`.

**Before/After**
```java
// Before
project(':share') {
  base {
    archivesName = "kafka-share"
  }

  dependencies {
    implementation project(':server-common')
    implementation libs.slf4jApi

    testImplementation project(':clients').sourceSets.test.output
    testImplementation project(':server-common').sourceSets.test.output


// After
        <subpackage name="share">
          <allow pkg="org.apache.kafka.server.share" />
          <subpackage name="persister">
            <allow pkg="org.apache.kafka.clients" />
            <allow pkg="org.apache.kafka.server.util" />
            <allow pkg="org.apache.kafka.test" />
          </subpackage>
        </subpackage>

```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18514: refactor share module code to server and server-common (#18524).

**Test**
Test modified in DefaultStatePersisterTest.java covering the corrected behaviour.
