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
package org.apache.cassandra.tools.compactionvalidator.compaction;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link SstableSetManager}.  Exercises directory layout creation,
 * hard-link replication of source files, and cleanup.
 */
public class SstableSetManagerTest
{
    private Path tmpRoot;

    @Before
    public void setUp() throws IOException
    {
        tmpRoot = Files.createTempDirectory("cv-sstableset-test-");
    }

    @After
    public void tearDown() throws IOException
    {
        if (tmpRoot != null && Files.exists(tmpRoot))
        {
            try (var stream = Files.walk(tmpRoot).sorted(Comparator.reverseOrder()))
            {
                for (Path p : (Iterable<Path>) stream::iterator)
                    Files.deleteIfExists(p);
            }
        }
    }

    @Test
    public void prepareCreatesAllFourDirectories()
    {
        SstableSetManager mgr = new SstableSetManager(tmpRoot.toFile(), 0xCAFEBABEDEADBEEFL);
        try
        {
            mgr.prepare();
        }
        catch (IOException e)
        {
            Assert.fail("prepare threw: " + e);
        }

        Assert.assertTrue(mgr.runDir().isDirectory());
        Assert.assertTrue(mgr.sourceDir().isDirectory());
        Assert.assertTrue(mgr.legacyOutputDir().isDirectory());
        Assert.assertTrue(mgr.cursorOutputDir().isDirectory());

        // Run dir name must contain the seed in the prescribed hex format.
        Assert.assertEquals("0xCAFEBABEDEADBEEF", mgr.runDir().getName());
    }

    @Test
    public void prepareIsIdempotent() throws IOException
    {
        SstableSetManager mgr = new SstableSetManager(tmpRoot.toFile(), 1L);
        mgr.prepare();
        mgr.prepare();
        Assert.assertTrue(mgr.runDir().isDirectory());
    }

    @Test
    public void hardLinkSourceTo_replicatesFiles() throws IOException
    {
        SstableSetManager mgr = new SstableSetManager(tmpRoot.toFile(), 42L);
        mgr.prepare();

        Path data = mgr.sourceDir().toPath().resolve("data.db");
        Files.writeString(data, "hello");

        File target = mgr.legacyOutputDir();
        mgr.hardLinkSourceTo(target);

        Path linked = target.toPath().resolve("data.db");
        Assert.assertTrue(Files.exists(linked));
        Assert.assertEquals("hello", Files.readString(linked));
    }

    @Test
    public void hardLinkSourceTo_replicatesNestedDirectories() throws IOException
    {
        SstableSetManager mgr = new SstableSetManager(tmpRoot.toFile(), 42L);
        mgr.prepare();

        Path nested = mgr.sourceDir().toPath().resolve("sub/dir/file.db");
        Files.createDirectories(nested.getParent());
        Files.writeString(nested, "nested");

        File target = mgr.cursorOutputDir();
        mgr.hardLinkSourceTo(target);

        Path linked = target.toPath().resolve("sub/dir/file.db");
        Assert.assertTrue(Files.exists(linked));
        Assert.assertEquals("nested", Files.readString(linked));
    }

    @Test
    public void cleanupAllRemovesEntireRunDir() throws IOException
    {
        SstableSetManager mgr = new SstableSetManager(tmpRoot.toFile(), 99L);
        mgr.prepare();

        Path file = mgr.sourceDir().toPath().resolve("a.db");
        Files.writeString(file, "x");
        Assert.assertTrue(Files.exists(file));

        mgr.cleanupAll();
        Assert.assertFalse(Files.exists(mgr.runDir().toPath()));
    }

    @Test
    public void cleanupAllOnMissingRunDirIsNoOp() throws IOException
    {
        SstableSetManager mgr = new SstableSetManager(tmpRoot.toFile(), 7L);
        // Note: prepare() was not called, so runDir does not exist.
        mgr.cleanupAll(); // must not throw
    }

    @Test
    public void preserveIsNoOpAndDoesNotThrow()
    {
        SstableSetManager mgr = new SstableSetManager(tmpRoot.toFile(), 0L);
        mgr.preserve();
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorRejectsNullWorkingDir()
    {
        new SstableSetManager(null, 0L);
    }
}
