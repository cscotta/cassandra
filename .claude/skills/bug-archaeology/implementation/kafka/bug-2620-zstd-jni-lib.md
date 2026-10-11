**Commit**: a4277c6617 · **Ticket**: KAFKA-17227
**Subsystem**: LICENSE-binary
**Category**: Logic error in condition

**Root cause**
* KAFKA-17227: Update zstd-jni lib
* Add note in upgrade docs
* Change zstd-jni version in docker native file and add warning in dependencies.gradle file
* Add reference to snappy in upgrade

Reviewers:  Chia-Ping Tsai <chia7712@gmail.com>,  Mickael Maison <mickael.maison@gmail.com>

**Before/After**
```java
// Before
zstd-jni-1.5.6-3 see: licenses/zstd-jni-BSD-2-clause
    "pattern":"\\Qlinux/amd64/libzstd-jni-1.5.6-3.so\\E"
    "pattern":"\\Qlinux/aarch64/libzstd-jni-1.5.6-3.so\\E"
  zstd: "1.5.6-3",

// After
zstd-jni-1.5.6-4 see: licenses/zstd-jni-BSD-2-clause
    "pattern":"\\Qlinux/amd64/libzstd-jni-1.5.6-4.so\\E"
    "pattern":"\\Qlinux/aarch64/libzstd-jni-1.5.6-4.so\\E"
<h4><a id="upgrade_3_9_0" href="#upgrade_3_9_0">Upgrading to 3.9.0 from any version 0.8.x through 3.8.x</a></h4>
    <h5><a id="upgrade_390_notable" href="#upgrade_390_notable">Notable changes in 3.9.0</a></h5>
    <ul>
        <li>In case you run your Kafka clusters with no execution permission for the <code>/tmp</code> partition, Kafka will not work properly. It might either refuse to start or fail
            when producing and consuming messages. This is due to the compression libraries <code>zstd-jni</code> and <code>snappy</code>.
            To remediate this problem you need to pass the following JVM flags to Kafka <code>ZstdTempFolder</code> and <code>org.xerial.snappy.tempdir</code> pointing to a directory with execution permissions.
            For example, this could be done via the <code>KAFKA_OPTS</code> environment variable like follows: <code>export KAFKA_OPTS="-DZstdTempFolder=/opt/kafka/tmp -Dorg.xerial.snappy.tempdir=/opt/kafka/tmp"</code>.
	    This is a known issue for version 3.8.0 as well.
        </li>
```

**How to spot**
When reviewing LICENSE-binary code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17227: update zstd-jni lib (#16763).

**Test**
No test added.
