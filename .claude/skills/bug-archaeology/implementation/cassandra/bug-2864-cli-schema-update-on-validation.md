# Bug 2864: CLI unconditionally updates server schema when setting column validator

**Commit**: 65ac893544 · **Ticket**: CASSANDRA-1635

## Detection Rule
When a function that is supposed to perform a local-only operation also calls a remote/server-side update method unconditionally, check whether the update should only happen on an explicit user action.

## Code Smell
A method called `updateColumnMetaData` throws `InvalidRequestException, TException` and calls `thriftClient_.system_update_column_family(columnFamily)` even when invoked from a read-only validation path. The caller guards with a `withUpdate` flag but the server call is inside the method itself.

## Tags
- scope: cassandra
- category: wrong-filter-result
- code-pattern: unconditional-side-effect
- severity: silent-wrong-result
- subsystem: tools-clients

## Root Cause
`updateColumnMetaData` was calling `thriftClient_.system_update_column_family` unconditionally, so any CLI operation that performed a local column validator lookup (e.g., `get`, `list`) also pushed the schema change to the server. The `withUpdate` flag in the caller was intended to gate this behaviour but the actual RPC was never conditioned on it.

## Example
```java
// BEFORE
private void updateColumnMetaData(CfDef columnFamily, byte[] columnName, String validationClass)
        throws InvalidRequestException, TException
{
    // ... update metadata list ...
    // saving information
    thriftClient_.system_update_column_family(columnFamily);  // always called!
}
```
```java
// AFTER
private void updateColumnMetaData(CfDef columnFamily, byte[] columnName, String validationClass)
{
    // ... update metadata list only — caller decides whether to push to server
    // thriftClient_ call removed entirely from this method
}
```
