# Bug 0073: FieldUtil Crashes with NoSuchFieldException on JDK17

**Commit**: 894536ab71 · **Ticket**: CASSANDRA-20247
**Subsystem**: tools/FieldUtil.java
**Category**: Missing null / bounds check

---

## Root Cause

`FieldUtil.setInstanceUnsafe` used `Field.class.getDeclaredField("modifiers")` to reflectively strip the `final` modifier from a field before assigning it. In JDK 17, the `modifiers` field was removed from the public reflection API, so this call throws `NoSuchFieldException` and the original code propagated it as a `RuntimeException` without any fallback. The fix adds a JDK 17-compatible path that calls the internal `Class.getDeclaredFields0(false)` method to scan all fields, including the hidden `modifiers` field, and uses the first match.

### Before / After

```java
// BEFORE
Field modifiersField = Field.class.getDeclaredField("modifiers");
modifiersField.setAccessible(true);
modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
```

```java
// AFTER
try {
    Field modifiers = Field.class.getDeclaredField("modifiers");
    modifiers.setAccessible(true);
    modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);
} catch (NoSuchFieldException t) {
    // jdk17 fallback
    Method getDeclaredFields0 = Class.class.getDeclaredMethod("getDeclaredFields0", boolean.class);
    getDeclaredFields0.setAccessible(true);
    Field[] fields = (Field[]) getDeclaredFields0.invoke(Field.class, false);
    for (Field f : fields) {
        if ("modifiers".equals(f.getName())) {
            f.setAccessible(true);
            f.setInt(field, field.getModifiers() & ~Modifier.FINAL);
            break;
        }
    }
}
```

---

## How to Spot

When code relies on a specific `Field` name obtained via `getDeclaredField("modifiers")` to manipulate reflection internals, it is tightly coupled to a JDK-version-specific implementation detail. During review, any `getDeclaredField("modifiers")` on `Field.class` itself should be flagged as a JDK-version compatibility hazard and a fallback path for JDK 17+ (where the field was made inaccessible) must be provided. The `catch` clause originally catching `NoSuchFieldException` and `IllegalAccessException` together masked the issue — separating the two exception types and adding a JDK-specific workaround for `NoSuchFieldException` is the correct pattern.

---

## Test

No test added.
