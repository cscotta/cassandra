# Bug 2564: OutboundTcpConnection silently swallows IOException as RuntimeException

**Commit**: ee320fba9d · **Ticket**: CASSANDRA-3235

## Detection Rule
When a network write method catches `IOException` and rethrows it as an unchecked `RuntimeException`, the caller cannot distinguish a transient IO failure from a programming error and cannot trigger reconnect or retry logic.

## Code Smell
An `IOException` in a serialization method is caught and rethrown as `new RuntimeException(e)`, hiding the original exception type from callers that handle `IOException` to initiate reconnection.

## Tags
- scope: cassandra
- category: silent-skip
- code-pattern: IOException-wrapped-as-RuntimeException
- severity: crash
- subsystem: network-messaging

## Root Cause
`OutboundTcpConnection.write()` internally caught `IOException` and wrapped it in `RuntimeException`. The caller (`run()` loop) caught `Exception` but the reconnect logic was keyed on detecting IO failures specifically. Wrapping the exception prevented proper handling: the connection would not be cleanly closed and re-established. Declaring `throws IOException` on the method and removing the try/catch lets the caller's existing IOException handler trigger reconnection correctly.

## Example
```java
// BEFORE
static void write(Message message, String id, DataOutputStream out) {
    try {
        out.writeInt(MessagingService.PROTOCOL_MAGIC);
        ...
    } catch (IOException e) {
        throw new RuntimeException(e); // swallows real type
    }
}
```
```java
// AFTER
static void write(Message message, String id, DataOutputStream out)
        throws IOException {
    out.writeInt(MessagingService.PROTOCOL_MAGIC);
    ...
}
```
