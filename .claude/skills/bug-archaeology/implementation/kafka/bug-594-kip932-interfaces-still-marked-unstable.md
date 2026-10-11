**Commit**: b443130002 · **Ticket**: KAFKA-19757

**Subsystem**: clients/src/main/java/org/apache/kafka/clients/admin/

**Category**: Wrong constant/default value

**Root cause**
Several KIP-932 and KIP-1043 public interfaces were still annotated `@Unstable` even though both KIPs had been accepted and fully implemented. Leaving them `@Unstable` signals to users that the API may change, which creates unnecessary uncertainty and prevents dependent tooling from treating the APIs as production-ready. Concurrently, some interfaces introduced in earlier KIPs had not yet been annotated with `@Deprecated` and a removal timeline even though they were superseded. The fix marks the relevant interfaces `@Stable` and adds `@Deprecated` annotations with `since` and `forRemoval=true` where applicable.

**Before**
```java
// Before
@Unstable
public interface AlterShareGroupOffsetsOptions { ... }
```

**After**
```java
// After
@Stable
public interface AlterShareGroupOffsetsOptions { ... }
```

**How to spot**
Search for `@Unstable` annotations on public interfaces in `clients/src/main/java`; any interface whose KIP has shipped and is production-ready should be upgraded to `@Stable`.

**Test**
Inspect the Javadoc output and confirm that `AlterShareGroupOffsetsOptions`, `DeleteShareGroupOffsetsOptions`, and related classes are no longer marked as unstable.
