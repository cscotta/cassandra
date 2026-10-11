# Bug 2722: FBUtilities.getInstance reflection call passes wrong arguments

**Commit**: cc0322fef8 · **Ticket**: n/a

## Detection Rule
When invoking a static no-arg method via reflection, pass an empty argument list to `invoke`, not a non-null array that is misinterpreted as varargs.

## Code Smell
`cls.getDeclaredMethod("getInstance").invoke(null, (Object) null)` passes `null` as the single argument, which is interpreted as a null argument to the invoked method rather than "no arguments", causing an `IllegalArgumentException` at runtime for any `getInstance()` with no parameters.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: reflection-wrong-varargs
- severity: crash
- subsystem: config

## Root Cause
`Method.invoke(Object obj, Object... args)` is variadic. Passing `(Object) null` as the args creates a single-element array containing `null`, which the JVM interprets as one null argument to the invoked method. For a no-argument `getInstance()` this causes an `IllegalArgumentException`. The fix uses `new Object[] {null, null}` — though the intent is a zero-arg call, the actual correct form would be `invoke(null)` or `invoke(null, new Object[0])`. Even so, the array form avoids the single-null-cast ambiguity.

## Example
```java
// BEFORE — null cast to Object is misinterpreted as one argument
rval = (T) cls.getDeclaredMethod("getInstance").invoke(null, (Object) null);
```
```java
// AFTER — explicit array avoids single-null-cast ambiguity
rval = (T) cls.getDeclaredMethod("getInstance").invoke(new Object[] {null, null});
```
