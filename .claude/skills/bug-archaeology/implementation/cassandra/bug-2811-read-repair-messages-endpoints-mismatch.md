# Bug 2811: Read Repair Sends Messages to Endpoints Not in Recipient List

**Commit**: c67a22ffe4 · **Ticket**: CASSANDRA-2010

## Detection Rule
When read repair is disabled and the recipient endpoint list is trimmed to `blockfor` entries but the messages array is sized to the original (larger) list length, the mismatched sizes cause `ArrayIndexOutOfBoundsException` or messages sent to null endpoints.

## Code Smell
`Message[] messages = new Message[targets]` where `targets` was set to `handler.blockfor` but `endpoints` still contained more entries — the loop that populates `messages[i]` using `endpoints.get(i)` would read beyond `targets` or vice versa.

## Tags
- scope: cassandra
- category: off-by-one
- code-pattern: mismatched-array-and-endpoint-list-sizes
- severity: crash
- subsystem: cql-query

## Root Cause
The read path in `StorageProxy` needed to send read requests to a subset of endpoints when read repair was disabled (only `blockfor` endpoints needed), but to all endpoints when read repair was enabled. The original code used an integer `targets` variable set to either `endpoints.size()` or `handler.blockfor`, and sized the `messages` array to `targets`. But `endpoints` was not trimmed when `targets < endpoints.size()`, so the subsequent loop indexing both `messages[i]` and `endpoints.get(i)` would be off. The fix trims `endpoints` itself with `subList(0, handler.blockfor)` when repair is disabled, then sizes `messages` to `endpoints.size()` — keeping both collections in sync.

## Example
```java
// BEFORE: messages sized by targets but endpoints untrimmed
int targets;
if (randomlyReadRepair(command)) {
    targets = endpoints.size();
} else {
    targets = handler.blockfor;  // endpoints still has more entries
}
Message[] messages = new Message[targets];
// loop: messages[i] paired with endpoints.get(i) — mismatch when targets < endpoints.size()
```
```java
// AFTER: endpoints trimmed to match messages
if (!randomlyReadRepair(command)) {
    endpoints = endpoints.subList(0, handler.blockfor); // trim endpoints
}
Message[] messages = new Message[endpoints.size()]; // always in sync
```
