# Bug 2757: BufferedRandomAccessFile Does Not Read Full Buffer

**Commit**: 0792766221 · **Ticket**: CASSANDRA-2241

## Detection Rule
When filling a read buffer from a channel, loop until `read == buffer.length` or EOF, because a single `channel.read()` call is not guaranteed to fill the buffer.

## Code Smell
`long bytesRead = channel.read(buffer)` is called once and its return value treated as the total bytes in the buffer; a short read silently truncates data for subsequent accesses.

## Tags
- scope: cassandra
- category: wrong-serialization
- code-pattern: partial-channel-read
- severity: silent-wrong-result
- subsystem: storage-engine

## Root Cause
`BufferedRandomAccessFile.reBuffer()` called `channel.read(buffer)` once and assumed all available bytes were returned. Non-blocking channels can return fewer bytes than requested. The fix replaces `ByteBuffer`-based channel reads with a loop using `super.read(buffer, read, buffer.length - read)` until EOF or the buffer is full, tracking `validBufferBytes` explicitly.

## Example
```java
// BEFORE
long bytesRead = channel.read(buffer);
hitEOF = (bytesRead < buffer.capacity());
bufferEnd = bufferOffset + bytesRead;
buffer.rewind();
```
```java
// AFTER
int read = 0;
while (read < buffer.length) {
    int n = super.read(buffer, read, buffer.length - read);
    if (n < 0) break;
    read += n;
}
validBufferBytes = read;
```
