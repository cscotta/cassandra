**Commit**: 0ada8fac68 · **Ticket**: KAFKA-17096
**Subsystem**: build.gradle
**Category**: Resource leak

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
from kafkatest.directory_layout.kafka_path import KafkaPathResolverMixin

    def __init__(self, context, num_nodes, kafka, topic, max_messages=-1, security_protocol="PLAINTEXT", tls_version=None):

// After
    // log4j-appender is not in core dependencies,
    // so we add it to dependant-testlibs to avoid ClassNotFoundException in running kafka_log4j_appender.py
    from (project(':log4j-appender').jar ) { "$buildDir/dependant-testlibs" }
from kafkatest.directory_layout.kafka_path import KafkaPathResolverMixin, CORE_DEPENDANT_TEST_LIBS_JAR_NAME
from kafkatest.version import DEV_BRANCH
    def __init__(self, context, num_nodes, kafka, topic, max_messages=-1, security_protocol="PLAINTEXT",
                 tls_version=None):
        # Since the core module does not contain the log4j-appender, we need to add it manually.
        core_dependant_test_libs_jar = self.path.jar(CORE_DEPENDANT_TEST_LIBS_JAR_NAME, DEV_BRANCH)
        cmd += "for file in %s; do CLASSPATH=$CLASSPATH:$file; done;" % core_dependant_test_libs_jar
        cmd += " export CLASSPATH;"
```

**How to spot**
When reviewing build.gradle code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17096 fix kafka_log4j_appender.py (#16559).

**Test**
Test modified in kafka_log4j_appender.py covering the corrected behaviour.
