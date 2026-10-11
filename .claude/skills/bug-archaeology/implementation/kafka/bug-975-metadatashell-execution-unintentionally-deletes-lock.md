**Commit**: e2500186cb · **Ticket**: KAFKA-19334
**Subsystem**: server-common/src
**Category**: Race condition / missing synchronization

**Root cause**
## Summary
- MetadataShell may deletes lock file unintentionally when it exists or
fails to acquire lock. If there's running server, this causes unexpected
result as below:
  * MetadataShell succeeds on 2nd run unexpectedly
  * Even worse, LogManager/RaftManager's lock also no longer work from
concurrent Kafka process startup

Reviewers: TengYao Chi <frankvicky@apache.org>

**Before/After**
```java
// Before
            fileLock.destroy();
                fileLock.destroy();
                log.error("Error destroying fileLock", e);
                    assertEquals("Unable to lock " + env.tempDir.getAbsolutePath() +
                        ". Please ensure that no broker or controller process is using this " +
                        "directory before proceeding.",
                        assertThrows(RuntimeException.class,
                            () -> env.shell.run(List.of())).
                                getMessage());

// After

    /**
     * Unlock the file and close the associated FileChannel
     */
    public synchronized void unlockAndClose() throws IOException {
        unlock();
        channel.close();
    }
            fileLock.unlockAndClose();
                fileLock.unlockAndClose();
                log.error("Error cleaning up fileLock", e);
                    // We had a bug where the shell can lock the directory unintentionally
```

**How to spot**
When reviewing server-common/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19334 metadatashell execution unintentionally deletes lock file (#19817).

**Test**
Test modified in MetadataShellIntegrationTest.java covering the corrected behaviour.
