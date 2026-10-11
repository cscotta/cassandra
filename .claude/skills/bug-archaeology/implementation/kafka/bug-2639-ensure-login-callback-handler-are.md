**Commit**: 51c1e1e147 · **Ticket**: KAFKA-17188
**Subsystem**: clients/src
**Category**: Race condition / missing synchronization

**Root cause**
Reviewers: TengYao Chi <kitingiao@gmail.com>, Rajini Sivaram <rajinisivaram@googlemail.com>

**Before/After**
```java
// Before
                         LoginMetadata<?> loginMetadata) throws LoginException {
        loginCallbackHandler.configure(configs, saslMechanism, jaasContext.configurationEntries());
        login.configure(configs, jaasContext.name(), jaasContext.configuration(), loginCallbackHandler);
        login.login();

// After
import java.util.List;
import java.util.concurrent.Callable;
import static java.util.Arrays.asList;

                 LoginMetadata<?> loginMetadata) throws LoginException {
        try (AutoCloseable loginResources = this::closeResources) {
            loginCallbackHandler.configure(configs, saslMechanism, jaasContext.configurationEntries());
            login.configure(configs, jaasContext.name(), jaasContext.configuration(), loginCallbackHandler);
            login.login();
        } catch (Exception e) {
            throw new LoginException(e.getMessage());
        }
```

**How to spot**
When reviewing clients/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17188: ensure login and callback handler are closed upon encountering loginexception (#16724).

**Test**
Test modified in LoginManagerTest.java covering the corrected behaviour.
