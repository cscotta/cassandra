**Commit**: 816209d187 · **Ticket**: KAFKA-10787
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
                               ':server-common',
                               ':transaction-coordinator',

  if(JavaVersion.current().isJava11Compatible() && project.path !in excludedSpotlessModules) {
import org.apache.kafka.common.errors.TimeoutException;
import org.apache.kafka.common.utils.KafkaThread;
import org.apache.kafka.common.utils.LogContext;
import org.apache.kafka.common.utils.Time;
import org.slf4j.Logger;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;

// After
  //  the task `removeUnusedImports` is implemented by google-java-format,
  //  and unfortunately the google-java-format version used by spotless 6.14.0 can't work with JDK 21.
  //  Hence, we apply spotless tasks only if the env is either JDK11 or JDK17
  if ((JavaVersion.current().isJava11() || (JavaVersion.current() == JavaVersion.VERSION_17)) && project.path !in excludedSpotlessModules) {

import org.apache.kafka.common.errors.TimeoutException;
import org.apache.kafka.common.utils.KafkaThread;
import org.apache.kafka.common.utils.LogContext;
import org.apache.kafka.common.utils.Time;

import org.slf4j.Logger;

```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-10787 apply spotless to transaction-coordinator and server-common (#16172).

**Test**
Test modified in DeferredEventQueueTest.java covering the corrected behaviour.
