/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.cassandra.db;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;

import org.junit.BeforeClass;
import org.junit.Test;

import org.apache.cassandra.SchemaLoader;
import org.apache.cassandra.config.CassandraRelevantProperties;
import org.apache.cassandra.db.marshal.BytesType;
import org.apache.cassandra.db.partitions.PartitionUpdate;
import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.cassandra.io.util.DataOutputBuffer;
import org.apache.cassandra.net.MessagingService;
import org.apache.cassandra.schema.KeyspaceParams;
import org.apache.cassandra.schema.TableMetadata;
import org.apache.cassandra.utils.FBUtilities;

import static org.apache.cassandra.utils.ByteBufferUtil.bytes;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Covers the contract of {@link Mutation.MutationSerializer}: the size a mutation reports is the
 * number of bytes its serialization writes, and only mutations below the caching limit keep the
 * serialized form.
 */
public class MutationSerializationTest
{
    private static final long CACHEABLE_MUTATION_SIZE_LIMIT = CassandraRelevantProperties.CACHEABLE_MUTATION_SIZE_LIMIT.getLong();

    private static final String KEYSPACE = "MutationSerializationTest";
    private static final String TABLE = "Standard";

    @BeforeClass
    public static void defineSchema() throws ConfigurationException
    {
        SchemaLoader.prepareServer();
        SchemaLoader.createKeyspace(KEYSPACE, KeyspaceParams.simple(1),
                                    SchemaLoader.standardCFMD(KEYSPACE, TABLE, 1, BytesType.instance));
    }

    @Test
    public void testSerializedSizeOfSmallMutation() throws Exception
    {
        Mutation mutation = mutation(16);
        assertSerializationInvariants(mutation, MessagingService.VERSION_60);
        assertTrue(Mutation.serializer.serializedSize(mutation, MessagingService.VERSION_60) < CACHEABLE_MUTATION_SIZE_LIMIT);
        assertEquals("CachedSerialization", cachedSerialization(mutation, MessagingService.VERSION_60));
    }

    @Test
    public void testSerializedSizeOfMutationAboveCacheLimit() throws Exception
    {
        Mutation mutation = mutation((int) CACHEABLE_MUTATION_SIZE_LIMIT);
        assertTrue(Mutation.serializer.serializedSize(mutation, MessagingService.VERSION_60) >= CACHEABLE_MUTATION_SIZE_LIMIT);
        assertSerializationInvariants(mutation, MessagingService.VERSION_60);
        assertEquals("SizeOnlyCacheableSerialization", cachedSerialization(mutation, MessagingService.VERSION_60));
    }

    /**
     * The caching decision is made on the serialized length, so the boundary is exactly the limit.
     */
    @Test
    public void testCacheLimitBoundary() throws Exception
    {
        int version = MessagingService.VERSION_60;

        Mutation belowLimit = mutationOfSerializedSize(CACHEABLE_MUTATION_SIZE_LIMIT - 1);
        assertEquals("CachedSerialization", cachedSerialization(belowLimit, version));

        Mutation atLimit = mutationOfSerializedSize(CACHEABLE_MUTATION_SIZE_LIMIT);
        assertEquals("SizeOnlyCacheableSerialization", cachedSerialization(atLimit, version));
    }

    /**
     * Every protocol version has its own cached serialization, and each reports the size it writes.
     */
    @Test
    public void testSerializedSizeAcrossProtocolVersions() throws Exception
    {
        for (int version : new int[]{ MessagingService.VERSION_40, MessagingService.VERSION_50, MessagingService.VERSION_60 })
        {
            Mutation mutation = mutation(32);
            assertSerializationInvariants(mutation, version);
            assertEquals("CachedSerialization", cachedSerialization(mutation, version));
        }
    }

    /**
     * Range tombstones and cell deletions take the same serialization path.
     */
    @Test
    public void testSerializedSizeWithTombstones() throws Exception
    {
        TableMetadata metadata = metadata();
        Mutation mutation = new RowUpdateBuilder(metadata, FBUtilities.timestampMicros(), bytes(1))
                            .clustering("c")
                            .addRangeTombstone("a", "b")
                            .delete("val")
                            .build();
        assertSerializationInvariants(mutation, MessagingService.VERSION_60);
    }

    private static void assertSerializationInvariants(Mutation mutation, int version) throws Exception
    {
        long size = Mutation.serializer.serializedSize(mutation, version);
        byte[] serialized = serialize(mutation, version);

        assertEquals("the reported size must be the number of bytes the serialization writes", serialized.length, size);

        // Neither the size nor the serialized form may change between calls.
        assertEquals(size, Mutation.serializer.serializedSize(mutation, version));
        assertEquals(size, mutation.serializedSize(version));
        assertArrayEquals(serialized, serialize(mutation, version));

        // The cached form must be the serialization of the mutation, byte for byte.
        try (DataOutputBuffer out = new DataOutputBuffer())
        {
            Mutation.MutationSerializer.serializeInternal(PartitionUpdate.serializer, mutation, out, version);
            assertArrayEquals(serialized, out.unsafeToByteArray());
        }
    }

    private static byte[] serialize(Mutation mutation, int version) throws Exception
    {
        try (DataOutputBuffer out = new DataOutputBuffer())
        {
            Mutation.serializer.serialize(mutation, out, version);
            return out.unsafeToByteArray();
        }
    }

    /**
     * The simple name of the {@link Mutation.Serializer} variant the mutation cached for this version.
     */
    private static String cachedSerialization(Mutation mutation, int version) throws Exception
    {
        Field field = Mutation.class.getDeclaredField("cachedSerializations");
        field.setAccessible(true);
        Object serialization = ((Object[]) field.get(mutation))[MessagingService.getVersionOrdinal(version)];
        return serialization == null ? null : serialization.getClass().getSimpleName();
    }

    private static Mutation mutationOfSerializedSize(long target) throws Exception
    {
        int version = MessagingService.VERSION_60;
        int valueSize = (int) Math.min(Integer.MAX_VALUE, target);
        Mutation mutation = mutation(valueSize);
        long size = Mutation.serializer.serializedSize(mutation, version);
        // the serialized size is a monotonic function of the value size with a slope close to 1,
        // so a couple of corrections land on the requested length exactly
        for (int i = 0; i < 4 && size != target; i++)
        {
            valueSize += (int) (target - size);
            mutation = mutation(valueSize);
            size = Mutation.serializer.serializedSize(mutation, version);
        }
        assertEquals("could not build a mutation whose serialized size is " + target, target, size);
        return mutation;
    }

    private static Mutation mutation(int valueSize)
    {
        return new RowUpdateBuilder(metadata(), FBUtilities.timestampMicros(), bytes(1))
               .clustering("c")
               .add("val", ByteBuffer.allocate(valueSize))
               .build();
    }

    private static TableMetadata metadata()
    {
        return Keyspace.open(KEYSPACE).getColumnFamilyStore(TABLE).metadata();
    }
}
