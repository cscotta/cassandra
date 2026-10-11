# Bug 0252: `LIST PERMISSIONS` Shows Wrong Resource Name for Functions with `/` in Name

**Commit**: 473656c1d5 · **Ticket**: CASSANDRA-17848
**Subsystem**: `auth/FunctionResource.java`, `cql3/functions/FunctionName.java`
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`FunctionResource.fromName()` split the resource string on every `/` character and then rejected strings with more than 3 parts (`parts.length > 3`). Because CQL allows quoted function names containing `/` (as well as `[` and `]`), such names would produce more than 3 parts on the naive split, causing an `IllegalArgumentException` and preventing the permission from being listed at all. Even when no exception occurred, the argument list was parsed by splitting on `[|]` which is a regex that matches `|` as an alternative to the surrounding brackets, not the literal bracket-pipe-bracket delimiter actually stored. The fix limits the initial split to at most 3 parts so that the function name portion is left intact, then locates the argument block by finding the last `[` character.

### Before / After

```java
// BEFORE
String[] parts = StringUtils.split(name, '/');
if (!parts[0].equals(ROOT_NAME) || parts.length > 3)
    throw new IllegalArgumentException(...);
String[] nameAndArgs = StringUtils.split(parts[2], "[|]");
```

```java
// AFTER
String[] parts = StringUtils.split(name, "/", 3);  // at most 3 splits
if (!parts[0].equals(ROOT_NAME))
    throw new IllegalArgumentException(...);
int lastStartingBracketIndex = function.lastIndexOf('[');
String functionName = StringUtils.substring(function, 0, lastStartingBracketIndex);
String functionArgs = StringUtils.substring(function, lastStartingBracketIndex + 1, function.length() - 1);
```

---

## How to Spot

Resource names that are constructed from user-supplied identifiers must be parsed with awareness that those identifiers can contain the delimiter. The safe pattern is to split with a maximum count so the final field is left verbatim, then extract sub-structure from the last field by searching for the innermost structural character (e.g., `lastIndexOf('[')`). Greedy unlimited splits on user-provided strings are a common source of parse failures.

---

## Test

`FunctionResourceTest` and `UFTest`/`AggregationTest` add cases for functions whose names contain `/`, `[`, and `]` characters, verifying that `LIST PERMISSIONS` round-trips the resource name correctly.
