# Bug 0160: system_views.settings Crashes on Array-Typed Config Properties

**Commit**: e4ae1f3a4f · **Ticket**: CASSANDRA-19475
**Subsystem**: src/java/org/apache/cassandra/db/virtual/SettingsTable.java
**Category**: Missing null / bounds check

---

## Root Cause

`SettingsTable.getValue(Property)` called `value.toString()` on every non-null config value, but Java's `Object.toString()` on arrays produces an identity hash string (e.g. `[Ljava.lang.String;@1a2b3c4d`) rather than the array's contents. Array-typed settings such as `data_file_directories` therefore appeared with unreadable hash strings in the virtual table. The fix introduces an `isArray()` check and uses `Arrays.asList(...).toString()` to render the elements correctly.

### Before / After

```java
// BEFORE
private String getValue(Property prop) {
    Object value = prop.get(config);
    return value == null ? null : value.toString();
}
```

```java
// AFTER
String getValue(Property prop) {
    Object value = prop.get(config);
    if (value == null) return null;
    if (value.getClass().isArray())
        return Arrays.asList((Object[]) value).toString();
    return value.toString();
}
```

---

## How to Spot

Any code that calls `Object.toString()` on a value whose runtime type may be an array is suspect: the contract of `toString()` is not overridden for arrays, so the result is the JVM's identity representation. During review, look for places that store or expose config/property values through a generic `Object` reference—if any of those properties can be multi-valued (arrays, lists), each formatting path needs an explicit type check.

---

## Test

Queries `SELECT value FROM vts.settings WHERE name = 'data_file_directories'` against a config that has two data directories and asserts the result equals `[/my/data/directory, /another/data/directory]`.
