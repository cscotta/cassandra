**Commit**: 5fe59f83e0 · **Ticket**: KAFKA-17741
**Subsystem**: clients/src
**Category**: State not cleaned up

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.common.utils.Java;
    public static final String DEFAULT_SSL_PROTOCOL;
    public static final String DEFAULT_SSL_ENABLED_PROTOCOLS;

    static {
        if (Java.IS_JAVA11_COMPATIBLE) {
            DEFAULT_SSL_PROTOCOL = "TLSv1.3";
            DEFAULT_SSL_ENABLED_PROTOCOLS = "TLSv1.2,TLSv1.3";
        } else {
            DEFAULT_SSL_PROTOCOL = "TLSv1.2";
            DEFAULT_SSL_ENABLED_PROTOCOLS = "TLSv1.2";
        }

// After
    public static final String DEFAULT_SSL_PROTOCOL = "TLSv1.3";
    public static final String DEFAULT_SSL_ENABLED_PROTOCOLS = "TLSv1.2,TLSv1.3";
    @Override
    @Override
    @Override
    @Override
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            MethodHandle unmapper = lookup.findVirtual(unsafeClass, "invokeCleaner",
                    methodType(void.class, ByteBuffer.class));
            Field f = unsafeClass.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            Object theUnsafe = f.get(null);
```

**How to spot**
When reviewing clients/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17741 cleanup code base for jdk 11 (#17441).

**Test**
Test modified in KafkaFutureTest.java covering the corrected behaviour.
