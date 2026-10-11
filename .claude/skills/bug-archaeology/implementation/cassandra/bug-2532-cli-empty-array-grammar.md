# Bug 2532: CLI grammar rejects empty column_metadata array `[]` due to `+` quantifier

**Commit**: 3942b06f05 · **Ticket**: CASSANDRA-3126

## Detection Rule
When an ANTLR grammar rule uses `+` (one-or-more) for a list that can legitimately be empty, change it to `*` (zero-or-more); otherwise the parser throws a recognition error when users attempt to clear the list via an empty array literal.

## Code Smell
`'[' (hashConstruct ','?)+ ']'` — the `+` quantifier requires at least one element, so the CLI command `WITH column_metadata = []` fails with a parse error instead of clearing the column metadata.

## Tags
- scope: cassandra
- category: wrong-constant
- code-pattern: grammar-plus-not-star-quantifier
- severity: crash
- subsystem: tools-clients

## Root Cause
The ANTLR grammar for `arrayConstruct` used the `+` quantifier, which requires one or more `hashConstruct` elements. A user attempting to remove all column metadata with `UPDATE COLUMN FAMILY ... WITH column_metadata = []` received a parse error. Changing `+` to `*` allows zero elements and enables the empty-array syntax.

## Example
```antlr
// BEFORE — requires at least one element
arrayConstruct
    : '[' (hashConstruct ','?)+ ']'
        -> ^(ARRAY (hashConstruct)+)
    ;
```
```antlr
// AFTER — zero or more elements allowed
arrayConstruct
    : '[' (hashConstruct ','?)* ']'
        -> ^(ARRAY (hashConstruct)*)
    ;
```
