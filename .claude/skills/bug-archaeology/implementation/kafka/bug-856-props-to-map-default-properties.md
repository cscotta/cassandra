**Commit**: 1cef5325ad · **Ticket**: KAFKA-19213

**Subsystem**: Core utilities / Utils

**Category**: Logic error in condition

**Root cause**
`Utils.propsToMap` converted a `Properties` object to a `Map<String, String>` by iterating `Properties.entrySet()`. The `Properties` class inherits from `Hashtable`, and `entrySet()` returns only the properties stored directly in the object, silently omitting any entries inherited from a defaults `Properties` instance. The fix switches to `propertyNames()`, which enumerates both direct and default properties, so that configurations injected via a default `Properties` chain are not dropped.

**Before/After**
```java
// Before: misses default properties
for (Map.Entry<Object, Object> entry : properties.entrySet()) {
    map.put((String) entry.getKey(), (String) entry.getValue());
}

// After: includes default properties
Enumeration<?> names = properties.propertyNames();
while (names.hasMoreElements()) {
    String key = (String) names.nextElement();
    map.put(key, properties.getProperty(key));
}
```

**How to spot**
Any call to `Properties.entrySet()` or `Properties.keySet()` is suspect because both omit default-chain entries. Prefer `propertyNames()` or `stringPropertyNames()` whenever a `Properties` object may have been constructed with a defaults argument.

**Test**
`UtilsTest` adds a case where a `Properties` object is constructed with a non-empty defaults instance and verifies that `propsToMap` returns all keys including those from the defaults.
