# Bug 2352: CQL3 BATCH prepared statements share a single bound-type array, causing type conflicts

**Commit**: 6d38881430 · **Ticket**: CASSANDRA-4202

## Detection Rule

When a composite statement (BATCH, multi-statement transaction) calls `prepare()` on each child statement independently, check whether each child allocates its own fresh `boundTypes` array or whether all children must write into a shared, correctly-sized array that spans all bind markers.

## Code Smell

`BatchStatement.prepare()` calls `statement.prepare()` on each child in a loop and accumulates the per-statement `boundTypes` lists with `addAll`, but each child's `prepare()` creates its own local array sized to `getBoundsTerms()` for *that* statement. Bind-marker indices are assigned relative to the full BATCH, so indices written by child 2 overwrite the portion of child 1's smaller array, producing an `ArrayIndexOutOfBoundsException` or silently losing type information.

## Tags

- scope: cassandra
- category: wrong-serialization
- code-pattern: per-statement-array-sizing-in-batch
- severity: crash
- subsystem: cql-query

## Root Cause

Each child `ModificationStatement.prepare()` was allocating its own `AbstractType[]` sized to `statement.getBoundsTerms()` and writing bind-marker types at indices that are global to the BATCH. When `BatchStatement` accumulated these per-statement arrays with `addAll`, the resulting type list was not aligned with the global bind-marker numbering. The fix passes a single shared `AbstractType[]` (sized to the total `getBoundsTerms()` of the BATCH) to each child's new `prepare(AbstractType[])` overload, so all children write their type entries at the correct global indices.

## Example

```java
// BEFORE (BatchStatement)
public ParsedStatement.Prepared prepare() throws InvalidRequestException {
    List<AbstractType<?>> boundTypes = new ArrayList<>();
    for (ModificationStatement statement : statements)
        boundTypes.addAll(statement.prepare().boundTypes); // each child sizes its own array
    return new ParsedStatement.Prepared(this, boundTypes);
}
```

```java
// AFTER (BatchStatement)
public ParsedStatement.Prepared prepare() throws InvalidRequestException {
    AbstractType[] boundTypes = new AbstractType[getBoundsTerms()]; // one shared array
    return prepare(boundTypes);
}
public ParsedStatement.Prepared prepare(AbstractType[] boundTypes) throws InvalidRequestException {
    for (ModificationStatement statement : statements)
        statement.prepare(boundTypes); // each child fills its slice in-place
    return new ParsedStatement.Prepared(this, Arrays.asList(boundTypes));
}
```
