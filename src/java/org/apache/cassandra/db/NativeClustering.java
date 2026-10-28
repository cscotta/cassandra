/*
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements.  See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership.  The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License.  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.apache.cassandra.db;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.marshal.AddressBasedNativeData;
import org.apache.cassandra.db.marshal.ByteBufferAccessor;
import org.apache.cassandra.db.marshal.IndexedValueHolder;
import org.apache.cassandra.db.marshal.NativeAccessor;
import org.apache.cassandra.db.marshal.NativeData;
import org.apache.cassandra.db.marshal.ValueAccessor;
import org.apache.cassandra.io.util.DataOutputPlus;
import org.apache.cassandra.utils.FBUtilities;
import org.apache.cassandra.utils.ObjectSizes;
import org.apache.cassandra.utils.concurrent.OpOrder;
import org.apache.cassandra.utils.memory.AddressBasedAllocator;
import org.apache.cassandra.utils.memory.HeapCloner;
import org.apache.cassandra.utils.memory.MemoryUtil;
import org.apache.cassandra.utils.memory.NativeEndianMemoryUtil;

public class NativeClustering implements Clustering<NativeData>, IndexedValueHolder<NativeData>
{
    private static final long EMPTY_SIZE = ObjectSizes.measure(new NativeClustering());

    private final long peer;

    private NativeClustering() { peer = 0; }

    public static int estimateAllocationSize(Clustering<?> clustering)
    {
        int count = clustering.size();
        int metadataSize = (count * 2) + 4;
        int dataSize = clustering.dataSize();
        int bitmapSize = ((count + 7) >>> 3);

        return metadataSize + dataSize + bitmapSize;
    }

    public NativeClustering(AddressBasedAllocator allocator, OpOrder.Group writeOp, Clustering<?> clustering)
    {
        int count = clustering.size();
        int metadataSize = (count * 2) + 4;
        int dataSize = clustering.dataSize();
        int bitmapSize = ((count + 7) >>> 3);

        assert count < 64 << 10;
        assert dataSize <= FBUtilities.MAX_UNSIGNED_SHORT : String.format("Data size %d >= %d", dataSize, FBUtilities.MAX_UNSIGNED_SHORT + 1);

        peer = allocator.allocate(metadataSize + dataSize + bitmapSize, writeOp);
        long bitmapStart = peer + metadataSize;
        NativeEndianMemoryUtil.setShort(peer, (short) count);
        NativeEndianMemoryUtil.setShort(peer + (metadataSize - 2), (short) dataSize); // goes at the end of the other offsets

        NativeEndianMemoryUtil.setByte(bitmapStart, bitmapSize, (byte) 0);
        long dataStart = peer + metadataSize + bitmapSize;
        int dataOffset = 0;
        for (int i = 0 ; i < count ; i++)
        {
            NativeEndianMemoryUtil.setShort(peer + 2 + i * 2, (short) dataOffset);

            ByteBuffer value = clustering.bufferAt(i);
            if (value == null)
            {
                long boffset = bitmapStart + (i >>> 3);
                int b = NativeEndianMemoryUtil.getByte(boffset);
                b |= 1 << (i & 7);
                NativeEndianMemoryUtil.setByte(boffset, (byte) b);
                continue;
            }

            assert value.order() == ByteOrder.BIG_ENDIAN;

            int size = value.remaining();
            NativeEndianMemoryUtil.setBytes(dataStart + dataOffset, value);
            dataOffset += size;
        }
    }

    public Kind kind()
    {
        return Kind.CLUSTERING;
    }

    public ClusteringPrefix<NativeData> clustering()
    {
        return this;
    }

    public int size()
    {
        return NativeEndianMemoryUtil.getUnsignedShort(peer);
    }

    public int dataSize()
    {
        int dataSizeOffset = (size() * 2) + 2; // metadataSize - 2
        return NativeEndianMemoryUtil.getUnsignedShort(peer + dataSizeOffset);
    }

    public NativeData get(int i)
    {
        return buildDataObject(i, AddressBasedNativeData::new);
    }

    public void writeValueSkippingNullAndEmpty(AbstractType<?> type, int i, DataOutputPlus out) throws IOException
    {
        if (!isEmpty(i)) // is null is checked as a part of isEmpty
            type.writeValue(this, i, NativeAccessor.instance, out);
    }

    public long writtenLengthSkippingNullAndEmpty(AbstractType<?> type, int i)
    {
        if (isEmpty(i)) // is null is checked as a part of isEmpty
            return 0;

        return type.writtenLength(this, i, NativeAccessor.instance);
    }

    @Override
    public int size(int i)
    {
        int size = size();
        if (isNull(peer, size, i))
            return 0;

        int startOffset = NativeEndianMemoryUtil.getUnsignedShort(peer + 2 + i * 2);
        int endOffset = NativeEndianMemoryUtil.getUnsignedShort(peer + 4 + i * 2);
        return (endOffset - startOffset);
    }

    public boolean isNull(int i)
    {
        return isNull(peer, size(), i);
    }

    @Override
    public void write(int i, DataOutputPlus out) throws IOException
    {
        int size = size();
        if (i >= size)
            throw new IndexOutOfBoundsException();

        int metadataSize = (size * 2) + 4;
        int bitmapSize = ((size + 7) >>> 3);
        long bitmapStart = peer + metadataSize;
        int b = NativeEndianMemoryUtil.getByte(bitmapStart + (i >>> 3));
        if ((b & (1 << (i & 7))) != 0)
            return;

        int startOffset = NativeEndianMemoryUtil.getUnsignedShort(peer + 2 + i * 2);
        int endOffset = NativeEndianMemoryUtil.getUnsignedShort(peer + 4 + i * 2);

        long address = bitmapStart + bitmapSize + startOffset;
        int length = endOffset - startOffset;
        out.writeMemory(address, length);
    }

    private static boolean isNull(long peer, int size, int i)
    {
        if (i >= size)
            throw new IndexOutOfBoundsException();

        int metadataSize = (size * 2) + 4;
        long bitmapStart = peer + metadataSize;
        int b = NativeEndianMemoryUtil.getByte(bitmapStart + (i >>> 3));
        return ((b & (1 << (i & 7))) != 0);
    }

    public boolean isEmpty(int i)
    {
        int size = size();
        if (isNull(peer, size, i))
            return true;

        int startOffset = NativeEndianMemoryUtil.getUnsignedShort(peer + 2 + i * 2);
        int endOffset = NativeEndianMemoryUtil.getUnsignedShort(peer + 4 + i * 2);
        return (endOffset - startOffset) == 0;
    }


    private ByteBuffer getByteBuffer(int i)
    {
        return buildDataObject(i, (long address, int length) -> MemoryUtil.getByteBuffer(address, length, ByteOrder.BIG_ENDIAN));
    }

    private interface DataObjectBuilder<D> {
        D build(long address, int length);
    }

    private <D> D buildDataObject(int i, DataObjectBuilder<D> builder)
    {
        int size = size();
        if (i >= size)
            throw new IndexOutOfBoundsException();

        int metadataSize = (size * 2) + 4;
        int bitmapSize = ((size + 7) >>> 3);
        long bitmapStart = peer + metadataSize;
        int b = NativeEndianMemoryUtil.getByte(bitmapStart + (i >>> 3));
        if ((b & (1 << (i & 7))) != 0)
            return null;

        int startOffset = NativeEndianMemoryUtil.getUnsignedShort(peer + 2 + i * 2);
        int endOffset = NativeEndianMemoryUtil.getUnsignedShort(peer + 4 + i * 2);

        long address = bitmapStart + bitmapSize + startOffset;
        int length = endOffset - startOffset;
        return builder.build(address, length);
    }

    /**
     * Compares this clustering's component {@code i} with {@code other}'s component {@code j} using
     * the type {@code types[i]}, reading both values straight from their off-heap address instead of
     * allocating the {@link NativeData} views that {@link #get(int)} would produce. Nulls order as in
     * {@link ClusteringComparator#compareComponent(int, Object, ValueAccessor, Object, ValueAccessor)},
     * and as there the type is only looked up once both components are known not to be null.
     */
    public int compareComponent(int i, NativeClustering other, int j, AbstractType<?>[] types)
    {
        int size = size();
        if (i >= size)
            throw new IndexOutOfBoundsException();

        int metadataSize = (size * 2) + 4;
        long bitmapStart = peer + metadataSize;
        int b = NativeEndianMemoryUtil.getByte(bitmapStart + (i >>> 3));
        boolean isNull = (b & (1 << (i & 7))) != 0;

        int otherSize = other.size();
        if (j >= otherSize)
            throw new IndexOutOfBoundsException();

        int otherMetadataSize = (otherSize * 2) + 4;
        long otherBitmapStart = other.peer + otherMetadataSize;
        int otherB = NativeEndianMemoryUtil.getByte(otherBitmapStart + (j >>> 3));
        boolean otherIsNull = (otherB & (1 << (j & 7))) != 0;

        if (isNull)
            return otherIsNull ? 0 : -1;
        if (otherIsNull)
            return 1;

        int startOffset = NativeEndianMemoryUtil.getUnsignedShort(peer + 2 + i * 2);
        int endOffset = NativeEndianMemoryUtil.getUnsignedShort(peer + 4 + i * 2);
        long dataStart = bitmapStart + ((size + 7) >>> 3);

        int otherStartOffset = NativeEndianMemoryUtil.getUnsignedShort(other.peer + 2 + j * 2);
        int otherEndOffset = NativeEndianMemoryUtil.getUnsignedShort(other.peer + 4 + j * 2);
        long otherDataStart = otherBitmapStart + ((otherSize + 7) >>> 3);

        return types[i].compareNative(dataStart + startOffset, endOffset - startOffset,
                                      otherDataStart + otherStartOffset, otherEndOffset - otherStartOffset);
    }

    /**
     * The length of component {@code i}, or -1 if it is null. Reading the length rather than calling
     * {@link #get(int)} lets {@link ClusteringComparator} compare a component against a value that is
     * not held off-heap without allocating the view.
     */
    int nativeLength(int i)
    {
        int size = size();
        if (i >= size)
            throw new IndexOutOfBoundsException();

        if (isNull(peer, size, i))
            return -1;

        return NativeEndianMemoryUtil.getUnsignedShort(peer + 4 + i * 2)
               - NativeEndianMemoryUtil.getUnsignedShort(peer + 2 + i * 2);
    }

    /**
     * The address of component {@code i}'s data. Only meaningful when {@link #nativeLength(int)} of the
     * same component is not negative; a zero length addresses the start of an empty component, which is
     * never dereferenced.
     */
    long nativeAddress(int i)
    {
        int size = size();
        if (i >= size)
            throw new IndexOutOfBoundsException();

        int metadataSize = (size * 2) + 4;
        long bitmapStart = peer + metadataSize;
        return bitmapStart + ((size + 7) >>> 3) + NativeEndianMemoryUtil.getUnsignedShort(peer + 2 + i * 2);
    }

    public NativeData[] getRawValues()
    {
        NativeData[] values = new NativeData[size()];
        for (int i = 0 ; i < values.length ; i++)
            values[i] = get(i);
        return values;
    }

    public ByteBuffer[] getBufferArray()
    {
        ByteBuffer[] values = new ByteBuffer[size()];
        for (int i = 0 ; i < values.length ; i++)
            values[i] = getByteBuffer(i);
        return values;
    }

    public ValueAccessor<NativeData> accessor()
    {
        return NativeAccessor.instance;
    }

    public long unsharedHeapSize()
    {
        return EMPTY_SIZE;
    }

    public long unsharedHeapSizeExcludingData()
    {
        return EMPTY_SIZE;
    }

    @Override
    public Clustering<?> ensureAccessorFactorySupport()
    {
        return retainable();
    }

    @Override
    public final int hashCode()
    {
        return ClusteringPrefix.hashCode(this);
    }

    @Override
    public final boolean equals(Object o)
    {
        return ClusteringPrefix.equals(this, o);
    }

    // data are copied to heap byte buffers to detach from a NativeAllocator lifecycle
    @Override
    public Clustering<?> retainable()
    {
        assert kind() == Kind.CLUSTERING; // tombstones are never stored natively

        // always extract
        ByteBuffer[] values = new ByteBuffer[size()];
        for (int i = 0; i < values.length; ++i)
        {
            ByteBuffer value = getByteBuffer(i);
            values[i] = value != null ? HeapCloner.instance.clone(value) : null;
        }

        return ByteBufferAccessor.instance.factory().clustering(values);
    }
}
