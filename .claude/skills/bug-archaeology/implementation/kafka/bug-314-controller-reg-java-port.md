## Commit
a3fae9cbfa

## Ticket
KAFKA-20348

## Subsystem
Controller / KRaft

## Category
State not cleaned up

## Root cause
`ControllerRegistrationManager` was rewritten from Scala to Java. During the port, the `clusterId` constructor parameter was inadvertently dropped. The Java version resolves `clusterId` differently (from a field populated during `start()`), so startup registration worked, but the Scala-Java constructor contract was broken for any callers passing `clusterId` directly.

## Before
```scala
// Scala
class ControllerRegistrationManager(clusterId: String, ...) { ... }
```

## After
```java
// Java – clusterId resolved internally
public class ControllerRegistrationManager {
    public ControllerRegistrationManager(...) { /* no clusterId param */ }
}
```

## How to spot
When porting Scala classes to Java, diff the constructor signatures carefully. Missing a parameter is easy to overlook if the class compiles without it (e.g., if it initializes from a field set later).

## Test
`ControllerRegistrationManagerTest` exercises the Java constructor and startup path.
