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

import java.util.Locale;

import org.junit.Test;

import static org.apache.cassandra.schema.SchemaConstants.isLocalSystemKeyspace;
import static org.apache.cassandra.schema.SchemaConstants.isNonVirtualSystemKeyspace;
import static org.apache.cassandra.schema.SchemaConstants.isReplicatedSystemKeyspace;
import static org.apache.cassandra.schema.SchemaConstants.isSystemKeyspace;
import static org.apache.cassandra.schema.SchemaConstants.isVirtualSystemKeyspace;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The system keyspace predicates are evaluated several times per read and write command, so they are
 * answered from the constant name arrays rather than by lower casing the name. These tests pin the
 * contract of that fast path: case insensitive classification, and no overlap with user keyspaces.
 */
public class SchemaConstantsTest
{
    private static final String[] LOCAL = { "system", "system_schema", "system_accord" };
    private static final String[] VIRTUAL = { "system_virtual_schema", "system_views", "system_metrics",
                                              "system_accord_debug", "system_accord_debug_remote" };
    private static final String[] REPLICATED = { "system_auth", "system_traces", "system_distributed",
                                                 "system_cluster_metadata" };
    private static final String[] USER = { "cassandra_easy_stress", "perf_kv_45706", "systemx", "systems",
                                           "system_schema2", "system_views_too", "system_", "_system", "sys" };

    @Test
    public void classifiesSystemKeyspaces()
    {
        for (String name : LOCAL)
        {
            assertTrue(name, isLocalSystemKeyspace(name));
            assertTrue(name, isSystemKeyspace(name));
            assertTrue(name, isNonVirtualSystemKeyspace(name));
            assertFalse(name, isVirtualSystemKeyspace(name));
            assertFalse(name, isReplicatedSystemKeyspace(name));
        }

        for (String name : VIRTUAL)
        {
            assertTrue(name, isVirtualSystemKeyspace(name));
            // isLocalSystemKeyspace intentionally covers virtual tables as well
            assertTrue(name, isLocalSystemKeyspace(name));
            assertTrue(name, isSystemKeyspace(name));
            assertFalse(name, isNonVirtualSystemKeyspace(name));
            assertFalse(name, isReplicatedSystemKeyspace(name));
        }

        for (String name : REPLICATED)
        {
            assertTrue(name, isReplicatedSystemKeyspace(name));
            assertTrue(name, isSystemKeyspace(name));
            assertTrue(name, isNonVirtualSystemKeyspace(name));
            assertFalse(name, isLocalSystemKeyspace(name));
            assertFalse(name, isVirtualSystemKeyspace(name));
        }
    }

    @Test
    public void classifiesUserKeyspaces()
    {
        for (String name : USER)
        {
            assertFalse(name, isLocalSystemKeyspace(name));
            assertFalse(name, isVirtualSystemKeyspace(name));
            assertFalse(name, isReplicatedSystemKeyspace(name));
            assertFalse(name, isSystemKeyspace(name));
            assertFalse(name, isNonVirtualSystemKeyspace(name));
        }
    }

    @Test
    public void isCaseInsensitive()
    {
        for (String name : LOCAL)
        {
            assertTrue(name, isLocalSystemKeyspace(name.toUpperCase(Locale.ROOT)));
            assertTrue(name, isLocalSystemKeyspace(capitalize(name)));
        }
        for (String name : VIRTUAL)
            assertTrue(name, isVirtualSystemKeyspace(name.toUpperCase(Locale.ROOT)));
        for (String name : REPLICATED)
        {
            assertTrue(name, isReplicatedSystemKeyspace(name.toUpperCase(Locale.ROOT)));
            assertTrue(name, isNonVirtualSystemKeyspace(capitalize(name)));
        }
        for (String name : USER)
        {
            assertFalse(name, isLocalSystemKeyspace(name.toUpperCase(Locale.ROOT)));
            assertFalse(name, isSystemKeyspace(capitalize(name)));
        }
    }

    @Test
    public void isIndependentOfTheDefaultLocale()
    {
        Locale previous = Locale.getDefault();
        try
        {
            // Turkish lower cases 'I' to a dotless 'i', the classification must not care
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertTrue(isReplicatedSystemKeyspace("SYSTEM_TRACES"));
            assertTrue(isLocalSystemKeyspace("System_Schema"));
            assertTrue(isVirtualSystemKeyspace("SYSTEM_VIEWS"));
            assertFalse(isSystemKeyspace("SYSTEMX"));
        }
        finally
        {
            Locale.setDefault(previous);
        }
    }

    @Test
    public void selectOptionsEmptinessTracksProvidedOptions() throws Exception
    {
        // the empty instance is what the per-request index validation short circuits on
        assertTrue(org.apache.cassandra.cql3.statements.SelectOptions.EMPTY.isEmpty());

        org.apache.cassandra.cql3.statements.SelectOptions options = new org.apache.cassandra.cql3.statements.SelectOptions();
        assertTrue(options.isEmpty());
        options.addProperty(org.apache.cassandra.cql3.statements.SelectOptions.INCLUDED_INDEXES,
                            com.google.common.collect.ImmutableSet.of(com.google.common.collect.ImmutableSet.of("ks", "idx")));
        assertFalse(options.isEmpty());
    }

    private static String capitalize(String name)
    {
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
