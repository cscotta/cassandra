**Commit**: 96f83be096 · **Ticket**: KAFKA-19861
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
Reviewers: Mickael Maison <mickael.maison@gmail.com>

**Before/After**
```java
// Before
        int length4 = length / 4;
            final int i4 = i * 4;
            int k = (data[i4 + 0] & 0xff) + ((data[i4 + 1] & 0xff) << 8) + ((data[i4 + 2] & 0xff) << 16) + ((data[i4 + 3] & 0xff) << 24);
        switch (length % 4) {
                h ^= (data[(length & ~3) + 2] & 0xff) << 16;
                h ^= (data[(length & ~3) + 1] & 0xff) << 8;
                h ^= data[length & ~3] & 0xff;

// After
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
    private static final VarHandle INT_HANDLE =
            MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);

        int length4 = length >> 2;
            final int i4 = i << 2;
            int k = (int) INT_HANDLE.get(data, i4);
        int index = length4 << 2;
        switch (length - index) {
                h ^= (data[index + 2] & 0xff) << 16;
                h ^= (data[index + 1] & 0xff) << 8;
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19861: optimization of the murmur2 hash computation (#20359).

**Test**
Test modified in UtilsTest.java covering the corrected behaviour.
