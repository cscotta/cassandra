**Commit**: 0a367aa1ec · **Ticket**: KAFKA-20297
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
We have exposed several classes in org.apache.kafka.common.utils as
public APIs, which can be misleading since not all classes in this
package are intended for external use. So move following class into internal module
- OperatingSystem
- Java
- Exit
- Shell
- LoggingSignalHandler

Reviewers: cychiu <cychiu8@gmail.com>, Chia-Ping Tsai
 <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.common.utils.Exit;
import org.apache.kafka.common.utils.Java;
import org.apache.kafka.common.utils.Shell;
package org.apache.kafka.common.utils;

import org.apache.kafka.common.utils.internals.KafkaThread;
package org.apache.kafka.common.utils;

// After
import org.apache.kafka.common.utils.internals.Exit;
import org.apache.kafka.common.utils.internals.Java;
import org.apache.kafka.common.utils.internals.Shell;
import org.apache.kafka.common.utils.internals.OperatingSystem;
package org.apache.kafka.common.utils.internals;
package org.apache.kafka.common.utils.internals;
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20297 move operatingsystem, java, exit... into internal (#22093).

**Test**
Test modified in ByteBufferUnmapperTest.java covering the corrected behaviour.
