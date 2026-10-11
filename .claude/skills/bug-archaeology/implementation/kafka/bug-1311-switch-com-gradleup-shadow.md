**Commit**: e3080684c0 · **Ticket**: KAFKA-18142
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
**Prologue (related PR's):**
-  https://github.com/apache/kafka/pull/16295
-  https://github.com/apache/kafka/pull/17218
-  https://github.com/apache/kafka/pull/16489 

**Action points:**
- switch shadow plugin from `io.github.goooler.shadow` to
`com.gradleup.shadow`
- upgrade plugin version from **8.1.3** to **8.3.5** (release notes:
https://gradleup.com/shadow/changes/#v8-3-5-2024-11-03)

**Rationale:** both previous one and currently used shadow plugins are
now deprecated (in favor of `com.gradleup.shadow`):
- `com.github.johnrengelman.shadow` maintenance was transferred to
`com.gradleup.shadow`:
https://github.com/GradleUp/shadow/tree/8.3.5?tab=readme-ov-file#gradle-shadow
- `io.github.g...

**Before/After**
```java
// Before
  id 'io.github.goooler.shadow' version '8.1.3' apply false
            apply plugin: 'io.github.goooler.shadow'
            project.shadow.component(mavenJava)
            // https://github.com/johnrengelman/shadow/issues/324
                if (xml.asNode().get('dependencies') == null) {
                  xml.asNode().appendNode('dependencies')
                }
                def dependenciesNode = xml.asNode().get('dependencies').get(0)
        exclude(dependency(it.group + ':' + it.name))
  apply plugin: 'io.github.goooler.shadow'

// After
  id 'com.gradleup.shadow' version '8.3.6' apply false
            apply plugin: 'com.gradleup.shadow'
            from components.shadow
            // https://github.com/GradleUp/shadow/issues/324
                def dependenciesNode = xml.asNode().get('dependencies') ?: xml.asNode().appendNode('dependencies')
  tasks.withType(GenerateModuleMetadata) {
    enabled = false
  }

        exclude(dependency(it))
  apply plugin: 'com.gradleup.shadow'
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18142 switch to `com.gradleup.shadow` (#18018).

**Test**
No test added.
