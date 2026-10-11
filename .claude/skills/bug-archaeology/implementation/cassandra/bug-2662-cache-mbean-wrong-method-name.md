# Bug 2662: Cache MBean size() method inaccessible via JMX because interface declares wrong name

**Commit**: c37f106874 · **Ticket**: CASSANDRA-2781

## Detection Rule
When exposing a method via JMX through an MBean interface, ensure the interface method name matches the JavaBean property naming convention (`getSize()` for a readable attribute named "Size") so the JMX infrastructure can expose it; a non-conforming name like `size()` may not be visible as a JMX attribute.

## Code Smell
`InstrumentingCacheMBean` declares `public int size()` but JMX attribute naming requires `getSize()` for the property "Size"; clients calling the MBean attribute "Size" get an error because no `getSize()` method is present in the implementation.

## Tags
- scope: cassandra
- category: missing-override
- code-pattern: jmx-mbean-wrong-method-name
- severity: crash
- subsystem: config

## Root Cause
`InstrumentingCacheMBean` declared the size accessor as `size()` while Java JMX conventions require getter methods to follow the `getXxx()` pattern for attribute exposure. The `NodeCmd` display code and JMX clients attempted to access the "Size" attribute, which requires a `getSize()` method. No such method existed in either the interface or the implementation, causing `AttributeNotFoundException` at runtime. The fix renames the interface method to `getSize()` and adds a `getSize()` implementation in `InstrumentingCache` that delegates to `size()`.

## Example
```java
// BEFORE (interface)
public int size();  // not a valid JMX attribute getter

// AFTER (interface)
public int getSize();  // valid JavaBean getter for JMX attribute "Size"

// AFTER (implementation)
public int getSize() { return size(); }
```
