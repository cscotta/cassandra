**Commit**: f02c28b21d · **Ticket**: KAFKA-17994
**Subsystem**: streams/src
**Category**: Resource leak

**Root cause**
Reviewers: Bill Bejeck <bill@confluent.io>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
                    } catch (final RuntimeException deserializationException) {
        } catch (final RuntimeException processingException) {
            } catch (final RuntimeException fatalUserException) {
        } catch (final RuntimeException serializationException) {
                serializationException);
        } catch (final RuntimeException serializationException) {

// After
                    } catch (final Exception deserializationException) {
                        // while Java distinguishes checked vs unchecked exceptions, other languages
                        // like Scala or Kotlin do not, and thus we need to catch `Exception`
                        // (instead of `RuntimeException`) to work well with those languages
        } catch (final Exception processingException) {
            // while Java distinguishes checked vs unchecked exceptions, other languages
            // like Scala or Kotlin do not, and thus we need to catch `Exception`
            // (instead of `RuntimeException`) to work well with those languages
            } catch (final Exception fatalUserException) {
                // while Java distinguishes checked vs unchecked exceptions, other languages
                // like Scala or Kotlin do not, and thus we need to catch `Exception`
                // (instead of `RuntimeException`) to work well with those languages
```

**How to spot**
When reviewing streams/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17994 checked exceptions are not handled (#17817).

**Test**
No test added.
