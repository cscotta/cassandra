**Commit**: 76a9df47ca · **Ticket**: KAFKA-17639
**Subsystem**: .github/workflows
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        java: [ 21, 11 ]  # If we change these, make sure to adjust ci-complete.yml
        java: [ 21, 11 ]
        } catch (ExecutionException e) {
        } catch (ExecutionException e) {
import java.security.AccessController
import javax.security.auth.Subject

// After
        java: [ 23, 11 ]  # If we change these, make sure to adjust ci-complete.yml
        java: [ 23, 11 ]
            // In Java 23, When a CompletableFuture is cancelled, get() will throw a CancellationException wrapping a
            // CancellationException, thus we need to unwrap it to maintain the KafkaFuture behaviour.
            // see https://bugs.openjdk.org/browse/JDK-8331987
        } catch (ExecutionException | CancellationException e) {
            // In Java 23, When a CompletableFuture is cancelled, get() will throw a CancellationException wrapping a
            // CancellationException, thus we need to unwrap it to maintain the KafkaFuture behaviour.
            // see https://bugs.openjdk.org/browse/JDK-8331987
        } catch (ExecutionException | CancellationException e) {
        } catch (CancellationException e) {
            // In Java 23, When a CompletableFuture is cancelled, getNow() will throw a CancellationException wrapping a
```

**How to spot**
When reviewing .github/workflows code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17639 add java 23 to ci build matrix (#17409).

**Test**
Test modified in SaslPlainSslEndToEndAuthorizationTest.scala covering the corrected behaviour.
