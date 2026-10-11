**Commit**: d5413fdb48 · **Ticket**: KAFKA-17856
**Subsystem**: checkstyle/import-control.xml
**Category**: Logic error in condition

**Root cause**
Reviewers: TengYao Chi <kitingiao@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
package kafka.admin;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
        String message = captureStandardStream(false, run(command));
        String message = captureStandardStream(false, run(command));
        message = captureStandardStream(false, run(command));
        String message = captureStandardStream(false, run(command));
        message = captureStandardStream(false, run(command));
            String message = captureStandardStream(false, run(command));
            message = captureStandardStream(false, run(command));


// After
    <allow pkg="org.apache.kafka.security" />
package org.apache.kafka.tools;

import kafka.admin.ConfigCommand;
import static org.apache.kafka.tools.ToolsTestUtils.captureStandardErr;
import static org.apache.kafka.tools.ToolsTestUtils.captureStandardOut;
        String message = captureStandardOut(run(command));
        String message = captureStandardOut(run(command));
        message = captureStandardOut(run(command));
        String message = captureStandardOut(run(command));
        message = captureStandardOut(run(command));
            String message = captureStandardOut(run(command));
```

**How to spot**
When reviewing checkstyle/import-control.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17856 move configcommandtest and configcommandintegrationtest to tool module (#17767).

**Test**
Test modified in ConfigCommandIntegrationTest.java covering the corrected behaviour.
