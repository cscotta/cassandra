**Commit**: b9882a0020 · **Ticket**: KAFKA-20302
**Subsystem**: core — kafka.network.Processor / SocketServer
**Category**: Resource leak

**Root cause**
In `Processor.processCompletedReceives()`, when parsing the request header or processing the request body throws an exception (e.g., unsupported API version, corrupt payload), the `NetworkReceive` buffer allocated from the bounded `MemoryPool` was not released. The buffer is released either automatically when `RequestChannel.Request` takes ownership (for non-delayed-allocation requests), or manually otherwise. The exception path bypassed both release mechanisms. Under a bounded `MemoryPool`, enough invalid requests could exhaust the pool and stall all subsequent request processing.

**Before/After**
```scala
// Before — exception exits try-block, receive never closed
private def processCompletedReceives(): Unit = {
  selector.completedReceives.forEach { receive =>
    try {
      val header = parseRequestHeader(...)  // may throw
      ...
    } catch { case e: Exception => ... }
  }
}

// After — receive.close() called in finally/catch to return buffer
var req: RequestChannel.Request = null
try {
  header = parseRequestHeader(...)
  ...
} catch {
  case e: Exception =>
    if (req == null) receive.close()  // return buffer if Request didn't take ownership
    throw e
}
```

**How to spot**
Any code that allocates from a bounded pool and has multiple exit paths (including exception paths) must explicitly release the allocation on every path where the normal consumer has not yet taken ownership. In `processCompletedReceives`, the contract is that `receive` is owned by the `Request` object only after successful construction; before that point, exceptions must close the receive.

**Test**
`SocketServerMemoryPoolTest` — new integration test sends requests with unsupported API version and corrupt body to a broker configured with a 100 KB `MemoryPool`, then asserts `availableMemory()` is the same before and after the requests.
