# Bug 0207: InaccessibleObjectException in Leak Detector When Traversing Module-Protected Fields

**Commit**: 000533cb63 · **Ticket**: CASSANDRA-18708
**Subsystem**: utils/concurrent (Ref / leak detector)
**Category**: Missing null / bounds check

---

## Root Cause

Cassandra's leak detector (`Ref.java`) traverses object graphs via reflection to find unreleased references. It called `field.setAccessible(true)` unconditionally on every discovered field before reading its value. Under Java 9+ module encapsulation, fields belonging to modules that have not been opened to the unnamed module throw `InaccessibleObjectException` when `setAccessible` is forced. The fix replaces the unconditional `setAccessible` call with `field.trySetAccessible()`, and falls back to `sun.misc.Unsafe.getObject/getObjectVolatile` (obtained via its own reflection bootstrap) when accessibility cannot be granted, thereby reading the field value without opening the module.

### Before / After

```java
// BEFORE – unconditional setAccessible; throws InaccessibleObjectException on Java 17
for (Field field : getAllFields(clazz)) {
    field.setAccessible(true);   // crashes for fields in sealed JDK modules
    fields.add(field);
}
// later:
Object nextObject = nextField.get(o);
```

```java
// AFTER – try reflective access first; fall back to Unsafe for inaccessible fields
public static Object getFieldValue(Object object, Field field) {
    if (field.trySetAccessible())
        return field.get(object);
    // fall back to Unsafe
    long offset = unsafe.objectFieldOffset(field);
    return isFinal || isVolatile
           ? unsafe.getObjectVolatile(object, offset)
           : unsafe.getObject(object, offset);
}
```

---

## How to Spot

Code that iterates all fields of arbitrary objects (e.g., diagnostic traversal, serialization, testing utilities) and blindly calls `setAccessible(true)` will silently break under Java 9+ strong module boundaries. During Java-version upgrades, audit every `setAccessible` call that may run against JDK-internal classes, and either add the appropriate `--add-opens` flag or use `trySetAccessible()` with a `sun.misc.Unsafe` fallback for diagnostic code where opening all modules is undesirable.

---

## Test

No test added.
