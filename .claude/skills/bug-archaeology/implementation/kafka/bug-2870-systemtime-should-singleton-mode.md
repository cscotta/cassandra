**Commit**: 133f2b0f31 · **Ticket**: KAFKA-16879
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Greg Harris <gharris1727@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
public class SystemTime implements Time {
    Time SYSTEM = new SystemTime();
import org.apache.kafka.common.utils.SystemTime;
            final SystemTime realTime = new SystemTime();
            final SystemTime realTime = new SystemTime();
import org.apache.kafka.common.utils.SystemTime;
        Sensor infoSensor = new Sensor(null, "infoSensor", null, INFO_CONFIG, new SystemTime(),
        infoSensor = new Sensor(null, "infoSensor", null, DEBUG_CONFIG, new SystemTime(),
        infoSensor = new Sensor(null, "infoSensor", null, TRACE_CONFIG, new SystemTime(),
        Sensor debugSensor = new Sensor(null, "debugSensor", null, INFO_CONFIG, new SystemTime(),
        debugSensor = new Sensor(null, "debugSensor", null, DEBUG_CONFIG, new SystemTime(),

// After
class SystemTime implements Time {
    private static final SystemTime SYSTEM_TIME = new SystemTime();

    public static SystemTime getSystemTime() {
        return SYSTEM_TIME;
    }
    private SystemTime() {

    }
    Time SYSTEM = SystemTime.getSystemTime();
import org.apache.kafka.common.utils.Time;
            final Time realTime = Time.SYSTEM;
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16879 systemtime should use singleton mode (#16266).

**Test**
Test modified in ConsumerCoordinatorTest.java covering the corrected behaviour.
