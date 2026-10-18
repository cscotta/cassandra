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

package org.apache.cassandra.schema;

import org.junit.BeforeClass;
import org.junit.Test;

import org.apache.cassandra.SchemaLoader;
import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.Keyspace;
import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.cassandra.tcm.ClusterMetadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * Covers the contract of {@link Schema#getKeyspaceInstance(String)}: distributed (user and replicated system)
 * keyspaces are resolved through the distributed schema, local system keyspaces through the local instances, and
 * virtual system keyspaces have no instance at all. The lookup must not depend on the order in which those three
 * groups are tested, which is what the fast path changes.
 */
public class SchemaKeyspaceInstanceTest
{
    private static final String KEYSPACE = "schema_instance_test";
    private static final String TABLE = "t1";

    @BeforeClass
    public static void defineSchema() throws ConfigurationException
    {
        SchemaLoader.prepareServer();
        SchemaLoader.createKeyspace(KEYSPACE, KeyspaceParams.simple(1), SchemaLoader.standardCFMD(KEYSPACE, TABLE));
    }

    /**
     * The reordering of the lookup relies on no local or virtual system keyspace name being resolvable through the
     * distributed schema; assert that precondition over every name those groups contain.
     */
    @Test
    public void systemKeyspacesAreNotInTheDistributedSchema()
    {
        for (String name : SchemaConstants.LOCAL_SYSTEM_KEYSPACE_NAMES)
            assertNull(name, ClusterMetadata.current().schema.getKeyspace(name));
        for (String name : SchemaConstants.VIRTUAL_SYSTEM_KEYSPACE_NAMES)
            assertNull(name, ClusterMetadata.current().schema.getKeyspace(name));
    }

    @Test
    public void resolvesLocalSystemKeyspace()
    {
        for (String name : new String[]{ SchemaConstants.SYSTEM_KEYSPACE_NAME, SchemaConstants.SCHEMA_KEYSPACE_NAME })
        {
            Keyspace keyspace = Schema.instance.getKeyspaceInstance(name);
            assertNotNull(name, keyspace);
            assertSame(name, Keyspace.open(name), keyspace);
            assertEquals(name, keyspace.getName());
            // a local system keyspace must be resolved locally, not from the distributed schema
            assertNull(name, ClusterMetadata.current().schema.getKeyspace(name));
        }
    }

    @Test
    public void resolvesUserKeyspace()
    {
        Keyspace keyspace = Schema.instance.getKeyspaceInstance(KEYSPACE);
        assertNotNull(keyspace);
        assertSame(Keyspace.open(KEYSPACE), keyspace);
        assertSame(ClusterMetadata.current().schema.getKeyspace(KEYSPACE), keyspace);

        TableMetadata table = Schema.instance.getKeyspaceMetadata(KEYSPACE).tables.getNullable(TABLE);
        assertNotNull(table);
        ColumnFamilyStore cfs = keyspace.getColumnFamilyStore(table.id);
        assertSame(cfs, Keyspace.openAndGetStore(table));
    }

    @Test
    public void virtualSystemKeyspacesHaveNoInstance()
    {
        for (String name : SchemaConstants.VIRTUAL_SYSTEM_KEYSPACE_NAMES)
            assertNull(name, Schema.instance.getKeyspaceInstance(name));
    }

    @Test
    public void unknownKeyspaceHasNoInstance()
    {
        assertNull(Schema.instance.getKeyspaceInstance("schema_instance_test_does_not_exist"));
        assertNull(Schema.instance.getKeyspaceInstance(""));
    }
}
