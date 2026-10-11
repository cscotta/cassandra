**Commit**: 6af937639d · **Ticket**: KAFKA-16076
**Subsystem**: connect/runtime
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        } catch (IOException | InterruptedException | TimeoutException | ExecutionException e) {

// After
        } catch (IOException | TimeoutException | ExecutionException e) {
        } catch (InterruptedException e) {
            log.error("Thread was interrupted forwarding REST request to {} :", url, e);
            Thread.currentThread().interrupt();
            throw new ConnectRestException(Response.Status.INTERNAL_SERVER_ERROR, "Thread was interrupted trying to forward REST request: " + e.getMessage(), e);
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;

        @Test
        public void testHttpRequestInterrupted() throws ExecutionException, InterruptedException, TimeoutException {
            Request req = mock(Request.class);
```

**How to spot**
When reviewing connect/runtime code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16076 fix missing thread interrupt call in restclient class  (#16282).

**Test**
Test modified in RestClientTest.java covering the corrected behaviour.
