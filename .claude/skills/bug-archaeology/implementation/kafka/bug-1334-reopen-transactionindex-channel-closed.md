**Commit**: 269e8892ad · **Ticket**: KAFKA-15931
**Subsystem**: storage/src
**Category**: Resource leak

**Root cause**
Cached TransactionIndex may get closed if interrupted, causing following calls to always fail with ClosedChannelException, and forcing process to be restarted. In order to avoid this issue, a new method is exposed by TransactionIndex to validate state of channel; and index is reopened if closed. Reviewers: Luke Chen <showuon@gmail.com>, Kamal Chandraprakash<kamal.chandraprakash@gmail.com>, Nikhil Ramakrishnan <ramakrishnan.nikhil@gmail.com>

**Before/After**
```java
// Before
                return offsetIndex.sizeInBytes() + timeIndex.sizeInBytes() + Files.size(txnIndex.file().toPath());
import java.util.Optional;
import java.util.OptionalLong;
    private volatile File file;
    // note that the file is not created until we need it
    private Optional<FileChannel> maybeChannel = Optional.empty();
    private OptionalLong lastOffset = OptionalLong.empty();
        this.file = file;
        if (file.exists())
            openChannel();
        return file;
        this.file = new File(parentDir, file.getName());

// After
            // If txn index does not exist on the source, it's an empty file on the index entry
                return offsetIndex.sizeInBytes() + timeIndex.sizeInBytes() + Files.size(txnIndex.path());
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
    private static final Logger log = LoggerFactory.getLogger(TransactionIndex.class);

    final TransactionIndexFile txnFile;
    private Long lastOffset = null;
        this.txnFile = new TransactionIndexFile(file.toPath());
    }
```

**How to spot**
When reviewing storage/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15931: reopen transactionindex if channel is closed (#15241).

**Test**
Test modified in TransactionIndexTest.java covering the corrected behaviour.
