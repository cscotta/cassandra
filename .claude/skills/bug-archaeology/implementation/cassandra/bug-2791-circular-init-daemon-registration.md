# Bug 2791: Circular Initialization — Daemon Registered Before Server Init

**Commit**: a2a2dda311 · **Ticket**: n/a (caused by CASSANDRA-1951 patch)

## Detection Rule
When a class constructor calls a method on a singleton that itself triggers initialization of a subsystem requiring the class to be fully constructed first, the resulting circular dependency causes `NullPointerException` or incorrect state.

## Code Smell
A constructor of `AbstractCassandraDaemon` calls `StorageService.instance.registerDaemon(this)`. If `StorageService.instance` initialization (a static field) triggers any code that reads from `DatabaseDescriptor` or the snitch, and the snitch (`PropertyFileSnitch`) itself needs `StorageService` during its static init, a circular initialization loop occurs.

## Tags
- scope: cassandra
- category: race-condition
- code-pattern: constructor-triggers-circular-init
- severity: crash
- subsystem: bootstrap-startup

## Root Cause
`AbstractCassandraDaemon()` was calling `StorageService.instance.registerDaemon(this)` in its constructor. `StorageService.instance` is a static singleton initialized via `new StorageService()`, which may load `DatabaseDescriptor` and the configured `IEndpointSnitch`. `PropertyFileSnitch` initialization indirectly accessed `StorageService`, creating a circular class-loading/initialization chain. Moving `registerDaemon` to just before `StorageService.instance.initServer()` in the `setup()` method breaks the cycle by deferring registration until after all static initialization is complete.

## Example
```java
// BEFORE: registration in constructor triggers circular init
public AbstractCassandraDaemon() {
    StorageService.instance.registerDaemon(this); // PropertyFileSnitch → StorageService circular
}
```
```java
// AFTER: registration deferred to setup(), after static init completes
// (constructor removed)
public void setup() throws IOException {
    // ... setup code ...
    StorageService.instance.registerDaemon(this); // safe: all statics already initialized
    try {
        StorageService.instance.initServer();
    }
```
