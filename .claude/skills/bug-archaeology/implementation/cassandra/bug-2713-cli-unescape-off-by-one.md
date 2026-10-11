# Bug 2713: CLI unescapeSQLString mishandles backslash sequences

**Commit**: a3ab11865b · **Ticket**: CASSANDRA-2492

## Detection Rule
When hand-rolling a string-escape parser, verify that the loop bounds correctly exclude the trailing delimiter and that every escape sequence is mapped.

## Code Smell
Custom `unescapeSQLString` has an off-by-one in the loop termination condition (`((j == 0) ? i : i + 1) < b.length()`) that causes the last character before the closing quote to be dropped; the fix delegates entirely to a battle-tested library.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: faulty-escape-parser
- severity: silent-wrong-result
- subsystem: tools-clients

## Root Cause
The homegrown escape parser used a conditional loop bound that was incorrect when the string was quoted: the termination condition `i + 1 < b.length()` stopped one character too early, silently dropping the last character inside quotes. The replacement uses `StringEscapeUtils.unescapeJava` (Apache Commons Lang) after stripping the enclosing single quotes, which correctly handles all Java escape sequences and avoids the boundary error.

## Example
```java
// BEFORE — hand-rolled parser with off-by-one
for (int i = j; ((j == 0) ? i : i + 1) < b.length(); i++) {
    if (b.charAt(i) == '\\' && i + 2 < b.length()) { ... }
    else { sb.append(b.charAt(i)); }
}
return sb.toString();
```
```java
// AFTER — strip quotes then delegate to library
if (b.charAt(0) == '\'' && b.charAt(b.length()-1) == '\'')
    b = b.substring(1, b.length()-1);
return StringEscapeUtils.unescapeJava(b);
```
