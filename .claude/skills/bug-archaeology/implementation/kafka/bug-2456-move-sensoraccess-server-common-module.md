**Commit**: 6836fa259e · **Ticket**: KAFKA-17320
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import kafka.server.SensorAccess;

import scala.runtime.BoxedUnit;

            sensor -> {
                sensor.add(metricName(), new SimpleRate(), getQuotaMetricConfig(quota));
                return BoxedUnit.UNIT;
            }
import kafka.server.SensorAccess;

import scala.runtime.BoxedUnit;


// After
import org.apache.kafka.server.quota.SensorAccess;
            sensor -> sensor.add(metricName(), new SimpleRate(), getQuotaMetricConfig(quota))
import org.apache.kafka.server.quota.SensorAccess;
import java.util.function.Consumer
import org.apache.kafka.server.quota.{ClientQuotaCallback, ClientQuotaEntity, ClientQuotaType, QuotaType, QuotaUtils, SensorAccess, ThrottleCallback, ThrottledChannel}
        sensor => registerQuotaMetrics(metricTags)(sensor)
  protected def getOrCreateSensor(sensorName: String, expirationTimeSeconds: Long, registerMetrics: Consumer[Sensor]): Sensor = {
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17320 move sensoraccess to server-common module (#16864).

**Test**
No test added.
