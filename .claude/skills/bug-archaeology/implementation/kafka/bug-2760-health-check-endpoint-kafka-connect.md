**Commit**: 27220d146c · **Ticket**: KAFKA-10816
**Subsystem**: clients/src
**Category**: Race condition / missing synchronization

**Root cause**
Reviewers: Greg Harris <gharris1727@gmail.com>

**Before/After**
```java
// Before
import java.util.concurrent.CompletableFuture;
        CompletableFuture<T> future,
public abstract class AbstractConnectCli<T extends WorkerConfig> {
     *             can be handled in {@link #processExtraArgs(Herder, Connect, String[])}
     * @param herder the {@link Herder} instance that can be used to perform operations on the Connect cluster
    protected void processExtraArgs(Herder herder, Connect connect, String[] extraArgs) {
    protected abstract Herder createHerder(T config, String workerId, Plugins plugins,
            Connect connect = startConnect(workerProps, extraArgs);
     * @param extraArgs any additional CLI arguments that may need to be processed via
     *                  {@link #processExtraArgs(Herder, Connect, String[])}
    public Connect startConnect(Map<String, String> workerProps, String... extraArgs) {
        Herder herder = createHerder(config, workerId, plugins, connectorClientConfigOverridePolicy, restServer, restClient);

// After
import java.util.concurrent.Future;
        Future<T> future,
 * @param <H> the type of {@link Herder} to be used
public abstract class AbstractConnectCli<H extends Herder, T extends WorkerConfig> {
     *             can be handled in {@link #processExtraArgs(Connect, String[])}
    public void processExtraArgs(Connect<H> connect, String[] extraArgs) {
    protected abstract H createHerder(T config, String workerId, Plugins plugins,
            Connect<H> connect = startConnect(workerProps);
            processExtraArgs(connect, extraArgs);
    public Connect<H> startConnect(Map<String, String> workerProps) {
        H herder = createHerder(config, workerId, plugins, connectorClientConfigOverridePolicy, restServer, restClient);
```

**How to spot**
When reviewing clients/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-10816: add health check endpoint for kafka connect (#16477).

**Test**
Test modified in BlockingConnectorTest.java covering the corrected behaviour.
