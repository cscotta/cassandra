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

import org.junit.BeforeClass;
import org.junit.Test;

import org.apache.cassandra.SchemaLoader;
import org.apache.cassandra.Util;
import org.apache.cassandra.cql3.statements.schema.CreateTableStatement;
import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.cassandra.schema.KeyspaceParams;
import org.apache.cassandra.schema.SchemaTestUtil;
import org.apache.cassandra.schema.TableMetadata;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

/**
 * Covers the store a {@link ReadCommand} resolves: it must be the store its table belongs to, it must be resolved
 * once, and a table that is dropped while the command is still around must fail the resolution instead of handing out
 * a store that was invalidated.
 */
public class ReadCommandStoreResolutionTest
{
    private static final String KEYSPACE = "read_command_store_test";

    @BeforeClass
    public static void defineSchema() throws ConfigurationException
    {
        SchemaLoader.prepareServer();
        SchemaLoader.createKeyspace(KEYSPACE, KeyspaceParams.simple(1));
    }

    private static ColumnFamilyStore createTable(String table)
    {
        TableMetadata metadata = CreateTableStatement.parse(String.format("CREATE TABLE %s.%s (k text PRIMARY KEY, v int)", KEYSPACE, table),
                                                            KEYSPACE)
                                                        .build();
        SchemaTestUtil.announceNewTable(metadata);
        return Keyspace.open(KEYSPACE).getColumnFamilyStore(metadata.name);
    }

    @Test
    public void resolvesTheStoreOfTheCommandsTable()
    {
        ColumnFamilyStore cfs = createTable("memoized");
        ReadCommand command = Util.cmd(cfs).build();
        assertSame(cfs, command.getColumnFamilyStore());
        assertSame(cfs, command.getColumnFamilyStore());
        assertSame(Keyspace.openAndGetStore(command.metadata()), command.getColumnFamilyStore());

        SchemaTestUtil.announceTableDrop(KEYSPACE, cfs.name);
    }

    @Test
    public void droppedTableIsNotResolvedFromTheMemoizedStore()
    {
        ColumnFamilyStore cfs = createTable("dropped");
        ReadCommand command = Util.cmd(cfs).build();

        assertSame(cfs, command.getColumnFamilyStore());

        SchemaTestUtil.announceTableDrop(KEYSPACE, cfs.name);
        try
        {
            command.getColumnFamilyStore();
            fail("dropped table must not resolve to the invalidated store");
        }
        catch (IllegalArgumentException expected)
        {
            // the same failure a fresh resolution reports
        }
    }
}
