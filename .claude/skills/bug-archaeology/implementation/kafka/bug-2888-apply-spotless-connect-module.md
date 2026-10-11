**Commit**: 79b9c44acd · **Ticket**: KAFKA-10787
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
                               ':connect:api',
                               ':connect:basic-auth-extension',
                               ':connect:file',
                               ':connect:json',
                               ':connect:mirror',
                               ':connect:mirror-client',
                               ':connect:test-plugins',
                               ':connect:transforms',
import org.apache.kafka.connect.errors.ConnectException;

import java.util.Objects;


// After
import org.apache.kafka.connect.errors.ConnectException;

import java.util.Objects;

import org.apache.kafka.common.utils.Utils;

import java.util.concurrent.Future;


```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-10787 apply spotless to connect module (#16299).

**Test**
Test modified in ConnectorTest.java covering the corrected behaviour.
