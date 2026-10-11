# Bug 3054: CLI "drop index" fails for numeric or quoted column names

**Commit**: 8a8e1746dd · **Ticket**: CASSANDRA-3054

## Detection Rule
When the CLI grammar allows numeric literals and string literals as column names, ensure the grammar rule for `columnName` includes all token types and that compiled AST text is unescaped before use.

## Code Smell
`columnName : Identifier` in the grammar and `statement.getChild(1).getText()` without `CliUtils.unescapeSQLString()` will fail to parse or correctly handle numeric-only or single-quoted column names in `drop index`.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: grammar-rule-missing-token-types
- severity: crash
- subsystem: tools-clients

## Root Cause
The CLI grammar's `columnName` rule only accepted an `Identifier` token, rejecting string literals and integer literals that are valid column names. Additionally, the compiled code extracted the raw AST text without unescaping SQL-quoted strings. The fix expands the grammar rule to include `StringLiteral | Identifier | IntegerPositiveLiteral | IntegerNegativeLiteral` and wraps the text extraction in `CliUtils.unescapeSQLString()`.

## Example
```java
// BEFORE (grammar)
columnName : Identifier ;

// BEFORE (CliClient.java)
String rawColumName = statement.getChild(1).getText();
```
```java
// AFTER (grammar)
columnName : (StringLiteral | Identifier | IntegerPositiveLiteral | IntegerNegativeLiteral) ;

// AFTER (CliClient.java)
String rawColumName = CliUtils.unescapeSQLString(statement.getChild(1).getText());
```
