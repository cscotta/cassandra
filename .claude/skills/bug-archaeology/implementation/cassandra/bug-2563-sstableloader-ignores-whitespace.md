# Bug 2563: sstableloader --ignores option fails on hostnames with surrounding whitespace

**Commit**: 5927193d39 · **Ticket**: CASSANDRA-3247

## Detection Rule
When a comma-separated list of hostnames is split and each token is passed directly to `InetAddress.getByName()` without trimming, any whitespace around a comma causes a DNS lookup failure or `UnknownHostException`.

## Code Smell
`InetAddress.getByName(node)` is called on a raw token from a `split(",")` result without calling `.trim()` first; the error message on failure omits enough context to diagnose the problem.

## Tags
- scope: cassandra
- category: wrong-filter-result
- code-pattern: untrimmed-split-token
- severity: crash
- subsystem: tools-clients

## Root Cause
The BulkLoader parsed `--ignores` as a comma-separated list and called `InetAddress.getByName(node)` on each element without trimming whitespace. Any value like `"host1, host2"` would attempt to resolve `" host2"` (with a leading space), fail, and abort with a confusing error. Additionally, the option definition was missing the argument metavar, causing Apache Commons CLI to not parse the option value correctly.

## Example
```java
// BEFORE
opts.ignores.add(InetAddress.getByName(node));
// error: errorMsg(e.getMessage(), options)  -- no context
```
```java
// AFTER
opts.ignores.add(InetAddress.getByName(node.trim()));
// error: errorMsg("Unknown host: " + e.getMessage(), options)
```
