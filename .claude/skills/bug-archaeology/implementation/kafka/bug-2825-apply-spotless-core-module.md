**Commit**: 96989e4b64 · **Ticket**: KAFKA-10787
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
                               ':core',
import com.yammer.metrics.core.Gauge;
import com.yammer.metrics.core.Timer;
import scala.Option;
import scala.collection.JavaConverters;
import java.util.Objects;
import static org.apache.kafka.server.config.ServerLogConfigs.LOG_DIR_CONFIG;
import com.yammer.metrics.core.Timer;
import scala.runtime.BoxedUnit;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

// After

import com.yammer.metrics.core.Gauge;
import com.yammer.metrics.core.Timer;

import java.util.Objects;
import scala.Option;
import scala.collection.JavaConverters;

import static org.apache.kafka.server.config.ServerLogConfigs.LOG_DIR_CONFIG;


import com.yammer.metrics.core.Timer;
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-10787 apply spotless to core module (#16392).

**Test**
Test modified in ConfigCommandIntegrationTest.java covering the corrected behaviour.
