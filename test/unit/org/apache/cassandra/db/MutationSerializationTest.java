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

import java.nio.ByteBuffer;

import org.junit.BeforeClass;
import org.junit.Test;

import org.apache.cassandra.SchemaLoader;
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

public class MutationSerializationTest
{
    // The default value of CassandraRelevantProperties.CACHEABLE_MUTATION_SIZE_LIMIT
    private static final int CACHEABLE_MUTATION_SIZE_LIMIT = 1_000_000;

    private static final String KEYSPACE = "MutationSerializationTest";
    private static final String TABLE = "Standard";

    @BeforeClass
    public static void defineSchema() throws ConfigurationException
    {
        SchemaLoader.prepareServer();
        SchemaLoader.createKeyspace(KEYSPACE, KeyspaceParams.simple(1),
                                    SchemaLoader.standardCFMD(KEYSPACE, TABLE, 1, BytesType.instance));
    }

    /**
     * A mutation below the caching limit reports the size of its cached serialized form.
     */
    @Test
    public void testSerializedSizeOfCacheableMutation() throws Exception
    {
        assertSerializationInvariants(mutation(16));
    }

    /**
     * A mutation above the caching limit only caches its size, which must still be the size of its
     * serialized form.
     */
    @Test
    public void testSerializedSizeOfMutationAboveCacheLimit() throws Exception
    {
        Mutation mutation = mutation(2 * CACHEABLE_MUTATION_SIZE_LIMIT);
        assertTrue(Mutation.serializer.serializedSize(mutation, MessagingService.current_version) > CACHEABLE_MUTATION_SIZE_LIMIT);
        assertSerializationInvariants(mutation);
    }

    private static void assertSerializationInvariants(Mutation mutation) throws Exception
    {
        int version = MessagingService.current_version;
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

    private static Mutation mutation(int valueSize)
    {
        TableMetadata metadata = Keyspace.open(KEYSPACE).getColumnFamilyStore(TABLE).metadata();
        return new RowUpdateBuilder(metadata, FBUtilities.timestampMicros(), bytes(1))
               .clustering("c")
               .add("val", ByteBuffer.allocate(valueSize))
               .build();
    }
}
