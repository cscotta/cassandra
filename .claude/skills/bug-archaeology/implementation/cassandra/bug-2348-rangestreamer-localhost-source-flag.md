# Bug 2348: RangeStreamer fails to set foundSource when localhost is the only source

**Commit**: 655a748502 · **Ticket**: CASSANDRA-4200

## Detection Rule

When a loop iterates over candidates and `continue`s past a special case (e.g., skipping localhost), check that any boolean flag tracking "at least one candidate was found" is updated before the `continue`.

## Code Smell

A `for` loop that calls `continue` on the localhost address does so without setting `foundSource = true` first, so after the loop finishes, `foundSource` is still `false` even though localhost was a valid source — causing a spurious "no source found" exception.

## Tags

- scope: cassandra
- category: logic-error
- code-pattern: flag-update-skipped-by-continue
- severity: crash
- subsystem: streaming

## Root Cause

In `RangeStreamer`, when building the source-to-range map, localhost addresses are deliberately skipped to avoid self-streaming. However, the `foundSource` flag, which guards against throwing "no source available", was only set inside the loop body after the early-`continue`. When localhost was the only source for a range, `foundSource` remained `false` and an exception was thrown during a `move` operation.

## Example

```java
// BEFORE
for (InetAddress address : rangesWithSources.get(range)) {
    if (address.equals(FBUtilities.getBroadcastAddress()))
        continue;           // foundSource never set
    for (ISourceFilter filter : sourceFilters) { ... }
}
```

```java
// AFTER
for (InetAddress address : rangesWithSources.get(range)) {
    if (address.equals(FBUtilities.getBroadcastAddress())) {
        foundSource = true; // we found one, just don't stream locally
        continue;
    }
    for (ISourceFilter filter : sourceFilters) { ... }
}
```
