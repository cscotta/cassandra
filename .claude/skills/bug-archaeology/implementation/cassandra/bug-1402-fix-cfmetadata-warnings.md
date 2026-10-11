# Bug 1402: Fix CFMetaData compaction-related warnings

**Commit**: 1b21aef815 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/config/CFMetaData.java
**Category**: Logic error in condition

---

## Root Cause

`CFMetaData.createCompactionStrategyInstance` validated the requested class with `strategyClass.equals(WrappingCompactionStrategy.class)`. This compared a `Class<AbstractCompactionStrategy>` against a raw `Class` literal, which generates an unchecked/ragged type-comparison warning and, more importantly, could evaluate incorrectly depending on classloader context. The correct guard is to compare the fully-qualified class name as a `String`, which is what the caller already has before reflection loads the class. Additionally, `getConstructor` was called with an explicit `new Class[] { ... }` array where varargs suffice, generating a redundant-array-creation warning.

### Before / After

```java
// BEFORE
if (strategyClass.equals(WrappingCompactionStrategy.class))
    throw new ConfigurationException("You can't set WrappingCompactionStrategy ...");

Constructor<? extends AbstractCompactionStrategy> constructor =
    compactionStrategyClass.getConstructor(new Class[] {
        ColumnFamilyStore.class,
        Map.class
    });
```

```java
// AFTER
if (className.equals(WrappingCompactionStrategy.class.getName()))
    throw new ConfigurationException("You can't set WrappingCompactionStrategy ...");

Constructor<? extends AbstractCompactionStrategy> constructor =
    compactionStrategyClass.getConstructor(ColumnFamilyStore.class, Map.class);
```

---

## How to Spot

When a method accepts the class name as a `String` and also resolves it to a `Class` object, any subsequent guard against a specific forbidden class should use the string form (`ClassName.class.getName()`) rather than comparing `Class` objects. Comparing `Class` objects risks classloader-dependent inequality and produces compiler warnings. Reviewers should flag `someClass.equals(Foo.class)` patterns in configuration validation code.

---

## Test

No test added.
