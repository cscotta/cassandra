# Bug 2582: PropertyFileSnitch crashes when topology properties file is inside a JAR

**Commit**: ba8f60154a · **Ticket**: CASSANDRA-3138

## Detection Rule
When `ResourceWatcher.watch()` is called on a resource path that resolves to a JAR entry rather than a plain filesystem file, and the watcher unconditionally calls `new File(path)` on the URL, it throws or silently fails to register the watcher, which may crash startup.

## Code Smell
`ResourceWatcher.watch(RACK_PROPERTY_FILENAME, runnable, 60000)` is called unconditionally without first verifying that the resource is a plain file; if the file is loaded from a JAR, the `File` object is invalid and the watcher throws a `ConfigurationException` or `IOException` at startup.

## Tags
- scope: cassandra
- category: missing-null-check
- code-pattern: jar-resource-as-plain-file
- severity: crash
- subsystem: bootstrap-startup

## Root Cause
`PropertyFileSnitch` always registered a `ResourceWatcher` for `cassandra-topology.properties`. When Cassandra was deployed as a JAR and the properties file was bundled inside, `FBUtilities.resourceToFile()` could not return a valid `File` path for a JAR entry and threw `ConfigurationException`. This crash prevented startup even though the snitch had successfully loaded the topology. Wrapping the watcher registration in a try/catch that logs a debug message and continues allows JAR-based deployments to work correctly.

## Example
```java
// BEFORE: always registers watcher, crashes on JAR resources
Runnable runnable = new WrappedRunnable() { ... };
ResourceWatcher.watch(RACK_PROPERTY_FILENAME, runnable, 60 * 1000);
```
```java
// AFTER: only watch if it's a plain file
try {
    FBUtilities.resourceToFile(RACK_PROPERTY_FILENAME);
    ResourceWatcher.watch(RACK_PROPERTY_FILENAME, runnable, 60 * 1000);
} catch (ConfigurationException ex) {
    logger.debug("... does not look like a plain file. Will not watch it.");
}
```
