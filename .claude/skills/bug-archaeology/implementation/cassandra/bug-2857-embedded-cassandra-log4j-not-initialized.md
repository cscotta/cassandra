# Bug 2857: log4j not initialised when EmbeddedCassandraService is used

**Commit**: 58e29caa28 · **Ticket**: CASSANDRA-2857

## Detection Rule
When log4j initialisation is placed in a `static {}` block of the abstract superclass, subclasses loaded directly (e.g. `CassandraDaemon` via embedding) skip the parent's static initializer if the subclass is loaded first without ever loading the parent first.

## Code Smell
A `static {}` initializer block in an abstract class that configures logging will silently not run when a concrete subclass is instantiated directly without the abstract class being explicitly referenced first.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: static-init-not-inherited
- severity: silent-wrong-result
- subsystem: bootstrap-startup

## Root Cause
`AbstractCassandraDaemon` placed log4j initialization in a `static {}` block expecting it to run for all subclasses. In Java, a subclass's static initialization does not re-run the superclass's static initializer after the superclass has already been loaded. When embedded code directly instantiated `CassandraDaemon`, the parent's static block might not execute in the required order. The fix extracts the logic into a `public static void initLog4j()` method and calls it explicitly from a `static {}` block in the concrete `CassandraDaemon`.

## Example
```java
// BEFORE (AbstractCassandraDaemon)
static {
    // log4j configuration — may not run when subclass is loaded first
    String config = System.getProperty("log4j.configuration", "log4j-server.properties");
    ...
}
```
```java
// AFTER (AbstractCassandraDaemon)
public static void initLog4j() {
    String config = System.getProperty("log4j.configuration", "log4j-server.properties");
    ...
}

// CassandraDaemon
static {
    AbstractCassandraDaemon.initLog4j();
}
```
