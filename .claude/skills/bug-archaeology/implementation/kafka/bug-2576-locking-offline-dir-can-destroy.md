**Commit**: 5d115fea3b · **Ticket**: KAFKA-17310
**Subsystem**: server-common/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    public FileLock(File file) {
        try {
            this.channel = FileChannel.open(file.toPath(), StandardOpenOption.CREATE, StandardOpenOption.READ,
                    StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    public synchronized void lock() {
        try {
            flock = channel.lock();
        } catch (IOException e) {
            throw new RuntimeException(e);

// After
    public FileLock(File file) throws IOException {
        this.channel = FileChannel.open(file.toPath(), StandardOpenOption.CREATE, StandardOpenOption.READ,
                StandardOpenOption.WRITE);
    public synchronized void lock() throws IOException {
        flock = channel.lock();
    public synchronized boolean tryLock() throws IOException {
    public synchronized void unlock() throws IOException {
            flock.release();
    public synchronized void destroy() throws IOException {
        if (file.exists() && file.delete()) {
            LOGGER.trace("Deleted {}", file.getAbsolutePath());
        channel.close();
```

**How to spot**
When reviewing server-common/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17310 locking the offline dir can destroy the broker exceptionally (#16856).

**Test**
No test added.
