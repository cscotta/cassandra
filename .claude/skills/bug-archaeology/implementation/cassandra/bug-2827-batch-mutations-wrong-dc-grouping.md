# Bug 2827: Batch mutations send wrong messages to DC due to flattened Multimap key

**Commit**: d718dddd6e · **Ticket**: CASSANDRA-1931

## Detection Rule
When grouping messages by datacenter for multi-DC writes, the data structure must key on both DC and message identity; a flat `Multimap<String, Pair<Message,InetAddress>>` loses per-message grouping and can route mismatched messages.

## Code Smell
`Multimap<String, Pair<Message, InetAddress>> dcMessages` stores multiple `(message, address)` pairs under a single DC key, but the consuming loop assumes all pairs under a DC share the same `Message` object (for building FORWARD headers). When different mutations share a DC, the first message gets all addresses appended, producing malformed or missing writes.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: wrong-grouping-key
- severity: silent-wrong-result
- subsystem: network-messaging

## Root Cause
The `sendMessages` loop iterated all `(message, address)` pairs for a DC and treated the first as the "primary" message, adding remaining addresses as forwarding headers. But a batch mutation can produce distinct `Message` objects for different mutations going to the same DC. The fix uses `Map<String, Multimap<Message, InetAddress>>` so each unique `Message` within a DC has its own set of destination addresses.

## Example
```java
// BEFORE
Multimap<String, Pair<Message, InetAddress>> dcMessages = HashMultimap.create();
dcMessages.put(dc, new Pair<>(unhintedMessage, destination));

// AFTER
Map<String, Multimap<Message, InetAddress>> dcMessages = new HashMap<>();
Multimap<Message, InetAddress> messages = dcMessages.get(dc);
if (messages == null) { messages = HashMultimap.create(); dcMessages.put(dc, messages); }
messages.put(unhintedMessage, destination);
```
