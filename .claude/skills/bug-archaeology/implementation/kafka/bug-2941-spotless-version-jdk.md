**Commit**: 62e5cce184 · **Ticket**: KAFKA-10787
**Subsystem**: README.md
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
The import order is a part of static check. please call `spotlessApply` to optimize the imports of Java codes before filing pull request :
  //  the minimum required JRE of 6.14.0+ is 11
  //  refer:https://github.com/diffplug/spotless/tree/main/plugin-gradle#requirements
  id 'com.diffplug.spotless' version "6.13.0" apply false
def spotlessApplyModules = ['']

  if (project.path in spotlessApplyModules) {

// After
The import order is a part of static check. please call `spotlessApply` (require JDK 11+) to optimize the imports of Java codes before filing pull request.
  //  Spotless 6.13.0 has issue with Java 21 (see https://github.com/diffplug/spotless/pull/1920), and Spotless 6.14.0+ requires JRE 11
  //  We are going to drop JDK8 support. Hence, the spotless is upgrade to newest version and be applied only if the build env is compatible with JDK 11.
  //  spotless 6.15.0+ has issue in runtime with JDK8 even through we define it with `apply:false`. see https://github.com/diffplug/spotless/issues/2156 for more details
  id 'com.diffplug.spotless' version "6.14.0" apply false
def excludedSpotlessModules = [':clients',
                               ':connect:api',
                               ':connect:basic-auth-extension',
                               ':connect:file',
                               ':connect:json',
                               ':connect:mirror',
                               ':connect:mirror-client',
```

**How to spot**
When reviewing README.md code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-10787 update spotless version and remove support jdk8 (#16176).

**Test**
No test added.
