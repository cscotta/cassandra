# Bug 2888: ColumnFamilyStoreMBeanIterator uses stale JMX object name

**Commit**: b5140e04bf · **Ticket**: CASSANDRA-1433

## Detection Rule
When a JMX MBean type name is changed, search all query strings and ObjectName patterns that reference the old name to ensure they are updated in every client and tool.

## Code Smell
`ColumnFamilyStoreMBeanIterator` queries JMX with `"org.apache.cassandra.db:type=ColumnFamilyStores,*"` but the MBean was registered under the new name `"org.apache.cassandra.db:type=ColumnFamilies,*"`, so the iterator returns an empty result set and `nodetool cfstats` reports nothing.

## Tags
- scope: cassandra
- category: wrong-constant
- code-pattern: stale-jmx-object-name
- severity: silent-wrong-result
- subsystem: tools-clients

## Root Cause
The JMX type name was changed from `ColumnFamilyStores` to `ColumnFamilies` in a refactor but the query string in `NodeProbe.ColumnFamilyStoreMBeanIterator` was not updated. The mismatch caused `queryNames` to return no matches, making all CF stats appear empty in `nodetool`.

## Example
```java
// BEFORE — old type name, returns empty
ObjectName query = new ObjectName("org.apache.cassandra.db:type=ColumnFamilyStores,*");
```
```java
// AFTER — matches registered name
ObjectName query = new ObjectName("org.apache.cassandra.db:type=ColumnFamilies,*");
```
