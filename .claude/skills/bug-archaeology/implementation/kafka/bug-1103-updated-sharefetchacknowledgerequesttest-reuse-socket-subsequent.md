**Commit**: f3a4a1b185 · **Ticket**: KAFKA-19241
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Currently in the tests in ShareFetchAcknowledgeRequestTest, subsequent
share fetch / share acknowledge requests creates a new socket everytime,
even when the requests are sent by the same member. In reality, a single
share consumer clisnet will reuse the same socket for all the share
related requests in its lifetime. This PR changes the behaviour in the
tests to align with reality and reuse the same socket for all requests
by the same share group member. Reviewers: Apoorv Mittal <apoorvmittal10@gmail.com>

**Before/After**
```java
// Before
  protected def connectAndReceiveWithoutClosingSocket[T <: AbstractResponse](
    request: AbstractRequest,
    destination: Int
  )(implicit classTag: ClassTag[T]): T = {
    val socket = IntegrationTestUtils.connect(brokerSocketServer(destination), cluster.clientListener())
    openSockets += socket
    IntegrationTestUtils.sendAndReceive[T](request, socket)
  }

  protected def connectAndReceiveWithoutClosingSocket[T <: AbstractResponse](
    request: AbstractRequest
  )(implicit classTag: ClassTag[T]): T = {

// After
  protected def connectAny(): Socket = {
    val socket: Socket = IntegrationTestUtils.connect(
      cluster.anyBrokerSocketServer(),
      cluster.clientListener()
    )
    openSockets += socket
    socket
  }

  protected def connect(destination: Int): Socket = {
    val socket: Socket = IntegrationTestUtils.connect(
      brokerSocketServer(destination),
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19241: updated tests in sharefetchacknowledgerequesttest to reuse the socket for subsequent requests (#19640).

**Test**
Test modified in GroupCoordinatorBaseRequestTest.scala covering the corrected behaviour.
