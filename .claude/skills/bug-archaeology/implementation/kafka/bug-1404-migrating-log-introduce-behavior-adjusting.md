**Commit**: d31cbf59de · **Ticket**: KAFKA-18831
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
fix the following behavior changes. 1) in log4j 1, users can't change the logger by parent if the logger is declared by properties explicitly. For example, `org.apache.kafka.controller` has level explicitly in the properties. Hence, we can't use "org.apache.kafka=INFO" to change the level of `org.apache.kafka.controller` to INFO.

**Before/After**
```java
// Before
      Configurator.setAllLevels(LogManager.ROOT_LOGGER_NAME, level)
        Configurator.setAllLevels(loggerName, level)
      Configurator.setAllLevels(LogManager.ROOT_LOGGER_NAME, null)
        Configurator.setAllLevels(loggerName, null)

// After
      Configurator.setLevel(LogManager.ROOT_LOGGER_NAME, level)
        Configurator.setLevel(loggerName, level)
    val nullLevel: Level = null
      Configurator.setLevel(LogManager.ROOT_LOGGER_NAME, nullLevel)
        Configurator.setLevel(loggerName, nullLevel)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18831 migrating to log4j2 introduce behavior changes of adjusting level dynamically (#18969).

**Test**
No test added.
