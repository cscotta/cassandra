# Bug 2484: Assertion error when forwarding write to a single-destination — always adds FORWARD_HEADER

**Commit**: 37e16faaf6 · **Ticket**: CASSANDRA-3539

## Detection Rule

When a message forwarding header is written even when there are zero additional destinations (the iterator is exhausted after consuming the first element), the header contains an empty byte array and the receiving node asserts `forwardBytes.length >= bytesPerInetAddress`.

## Code Smell

`FastByteArrayOutputStream bos = new FastByteArrayOutputStream()` and `message.withHeaderAdded(FORWARD_HEADER, bos.toByteArray())` executed unconditionally before checking if `iter.hasNext()` — adding an empty-body forward header when there is only one destination.

## Tags

- scope: cassandra
- category: logic-error
- code-pattern: unconditional-header-add-with-empty-body
- severity: crash
- subsystem: network-messaging

## Root Cause

In `StorageProxy`, the code created a `DataOutputStream` for forwarding addresses and added the `FORWARD_HEADER` before checking `iter.hasNext()`. When the write had only one target (the first destination consumed by `iter.next()`), the forward header would contain zero bytes. The receiving `RowMutationVerbHandler` then asserted `forwardBytes.length >= bytesPerInetAddress`, which failed with an empty array.

## Example

```java
// BEFORE
FastByteArrayOutputStream bos = new FastByteArrayOutputStream();
DataOutputStream dos = new DataOutputStream(bos);
while (iter.hasNext())
{
    InetAddress destination = iter.next();
    dos.write(destination.getAddress());
}
message = message.withHeaderAdded(RowMutation.FORWARD_HEADER, bos.toByteArray());
```

```java
// AFTER
if (iter.hasNext())
{
    FastByteArrayOutputStream bos = new FastByteArrayOutputStream();
    DataOutputStream dos = new DataOutputStream(bos);
    while (iter.hasNext())
    {
        InetAddress destination = iter.next();
        dos.write(destination.getAddress());
    }
    message = message.withHeaderAdded(RowMutation.FORWARD_HEADER, bos.toByteArray());
}
```
