# Bug 2944: Local multi-get commands run serially instead of on read stage

**Commit**: 050752c703 · **Ticket**: CASSANDRA-555

## Detection Rule
When a method processes a list of work items in a loop with direct synchronous calls, check whether the work items could be dispatched in parallel to a thread pool instead.

## Code Smell
`weakReadLocal` iterates `commands` calling `command.getRow(table)` synchronously in-line, forcing sequential execution instead of submitting each as a `Callable` to `StageManager.readStage_`.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: serial-instead-of-parallel
- severity: performance
- subsystem: cql-query

## Root Cause
The `weakReadLocal` method in `StorageProxy` read each local command one after another in a single loop rather than submitting them as parallel futures to the read executor. Multi-get requests (fetching multiple keys) therefore ran with no concurrency even when multiple reads could overlap I/O. The fix wraps each `ReadCommand` in a `Callable` and collects `Future`s before joining them.

## Example
```java
// BEFORE
for (ReadCommand command: commands) {
    Table table = Table.open(command.table);
    Row row = command.getRow(table);
    if (row != null)
        rows.add(row);
}
```
```java
// AFTER
for (ReadCommand command: commands) {
    Callable<Object> callable = new weakReadLocalCallable(command);
    futures.add(StageManager.getStage(StorageService.readStage_).execute(callable));
}
for (Future<Object> future : futures) {
    rows.add((Row) future.get());
}
```
