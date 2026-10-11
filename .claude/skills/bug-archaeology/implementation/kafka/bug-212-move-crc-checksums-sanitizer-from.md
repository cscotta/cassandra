**Commit**: e7a568863e · **Ticket**: KAFKA-20297
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Summary: move Crc32C Checksums and Sanitizer from utils to
utils.internals

Reviewers: Ken Huang <s7133700@gmail.com>, Chia-Ping Tsai
 <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.common.utils.Sanitizer;
import org.apache.kafka.common.utils.Crc32C;
import org.apache.kafka.common.utils.Checksums;
package org.apache.kafka.common.utils;
package org.apache.kafka.common.utils;
package org.apache.kafka.common.utils;

// After
import org.apache.kafka.common.utils.internals.Sanitizer;
import org.apache.kafka.common.utils.internals.Crc32C;
import org.apache.kafka.common.utils.internals.Checksums;
import org.apache.kafka.common.utils.internals.Sanitizer;
package org.apache.kafka.common.utils.internals;
package org.apache.kafka.common.utils.internals;
package org.apache.kafka.common.utils.internals;
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20297 move crc32c checksums and sanitizer from utils to utils.internals (#22072).

**Test**
Test modified in ChecksumsTest.java covering the corrected behaviour.
