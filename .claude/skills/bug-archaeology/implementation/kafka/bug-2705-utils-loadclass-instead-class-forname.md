**Commit**: c97421c100 · **Ticket**: KAFKA-17150
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Signed-off-by: Greg Harris <greg.harris@aiven.io>
Reviewers: Chris Egerton <chrise@aiven.io>, Chia-Ping Tsai <chia7712@gmail.com>, Josep Prat <josep.prat@aiven.io>

**Before/After**
```java
// Before
                    klass = Class.forName((String) klass, true, Utils.getContextOrKafkaClassLoader());
                        ClassLoader contextOrKafkaClassLoader = Utils.getContextOrKafkaClassLoader();
                        // Use loadClass here instead of Class.forName because the name we use here may be an alias
                        // and not match the name of the class that gets loaded. If that happens, Class.forName can
                        // throw an exception.
                        Class<?> klass = contextOrKafkaClassLoader.loadClass(trimmed);
                        // Invoke forName here with the true name of the requested class to cause class
                        // initialization to take place.
                        return Class.forName(klass.getName(), true, contextOrKafkaClassLoader);
        return Class.forName(klass, true, Utils.getContextOrKafkaClassLoader()).asSubclass(base);
            Class<?> c = Class.forName(className, true, Utils.getContextOrKafkaClassLoader());

// After
                    klass = Utils.loadClass((String) klass, Object.class);
                        return Utils.loadClass(trimmed, Object.class);
        ClassLoader contextOrKafkaClassLoader = Utils.getContextOrKafkaClassLoader();
        // Use loadClass here instead of Class.forName because the name we use here may be an alias
        // and not match the name of the class that gets loaded. If that happens, Class.forName can
        // throw an exception.
        Class<?> loadedClass = contextOrKafkaClassLoader.loadClass(klass);
        // Invoke forName here with the true name of the requested class to cause class
        // initialization to take place.
        return Class.forName(loadedClass.getName(), true, contextOrKafkaClassLoader).asSubclass(base);
            Class<?> c = Utils.loadClass(className, Object.class);
import org.apache.kafka.common.utils.Utils;
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17150: use utils.loadclass instead of class.forname to resolve aliases correctly (#16608).

**Test**
Test modified in PluginsTest.java covering the corrected behaviour.
