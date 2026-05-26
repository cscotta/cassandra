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
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;

/**
 * Manages working directories for one validator run.
 *
 * <p>For a root seed {@code S}, this class creates the directory layout:
 * <pre>
 *   &lt;workingDir&gt;/&lt;seed-hex&gt;/
 *   &lt;workingDir&gt;/&lt;seed-hex&gt;/source/
 *   &lt;workingDir&gt;/&lt;seed-hex&gt;/output-legacy/
 *   &lt;workingDir&gt;/&lt;seed-hex&gt;/output-cursor/
 * </pre>
 *
 * <p>The {@code source/} directory holds the originally generated SSTables for the
 * run.  Both compaction backends operate on hard-linked copies of those files
 * placed under {@code output-legacy/} and {@code output-cursor/}, ensuring that
 * the two pipelines see byte-identical inputs and that mutations on one side do
 * not affect the other.
 *
 * <p>The hex encoding of the seed is upper-case, sixteen-digit, zero-padded
 * (e.g. {@code 0xCAFEBABEDEADBEEF}). This is suitable both for filenames and for
 * direct logging.
 */
public final class SstableSetManager
{
    private final Path runDir;
    private final Path sourceDir;
    private final Path legacyDir;
    private final Path cursorDir;

    /**
     * Constructs a manager for the run with the given root seed under the given
     * working directory.
     *
     * @param workingDir top-level working directory shared across runs
     * @param rootSeed   64-bit root seed for this run; encoded as a 16-character
     *                   upper-case zero-padded hex string in the path
     */
    public SstableSetManager(File workingDir, long rootSeed)
    {
        if (workingDir == null)
            throw new IllegalArgumentException("workingDir must not be null");
        String seedHex = String.format("0x%016X", rootSeed);
        this.runDir = workingDir.toPath().resolve(seedHex);
        this.sourceDir = runDir.resolve("source");
        this.legacyDir = runDir.resolve("output-legacy");
        this.cursorDir = runDir.resolve("output-cursor");
    }

    /**
     * @return the directory holding the originally generated SSTables for this run
     */
    public File sourceDir()
    {
        return sourceDir.toFile();
    }

    /**
     * @return the directory in which the legacy (iterator) compaction reads/writes
     */
    public File legacyOutputDir()
    {
        return legacyDir.toFile();
    }

    /**
     * @return the directory in which the cursor compaction reads/writes
     */
    public File cursorOutputDir()
    {
        return cursorDir.toFile();
    }

    /**
     * @return the parent directory containing all per-side directories for this run
     */
    public File runDir()
    {
        return runDir.toFile();
    }

    /**
     * Creates the four directories that make up this run's working layout.
     *
     * <p>This is idempotent: {@link Files#createDirectories(Path, java.nio.file.attribute.FileAttribute[])}
     * does nothing if the directory already exists.
     *
     * @throws IOException if any directory cannot be created
     */
    public void prepare() throws IOException
    {
        Files.createDirectories(runDir);
        Files.createDirectories(sourceDir);
        Files.createDirectories(legacyDir);
        Files.createDirectories(cursorDir);
    }

    /**
     * Recursively hard-links every regular file under {@link #sourceDir()} into
     * {@code target}, preserving relative paths.
     *
     * <p>If {@link Files#createLink(Path, Path)} fails because {@code source} and
     * {@code target} live on different filesystems (or the filesystem does not
     * support hard links — see {@code EXDEV} {@link IOException}), the entry is
     * copied with {@link Files#copy(Path, Path, java.nio.file.CopyOption...)}.
     *
     * <p>Subdirectories under {@code source/} are reproduced as plain directories
     * under {@code target} (we link files, not directories).
     *
     * @param target destination directory; must already exist
     * @throws IOException if directory walking, linking, or copying fails for a
     *                     reason other than cross-device link unsupported
     */
    public void hardLinkSourceTo(File target) throws IOException
    {
        if (target == null)
            throw new IllegalArgumentException("target must not be null");
        Path targetRoot = target.toPath();
        Files.createDirectories(targetRoot);

        Files.walkFileTree(sourceDir, new FileVisitor<Path>()
        {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
            {
                Path rel = sourceDir.relativize(dir);
                Path destDir = targetRoot.resolve(rel.toString());
                Files.createDirectories(destDir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
            {
                Path rel = sourceDir.relativize(file);
                Path destFile = targetRoot.resolve(rel.toString());
                Files.createDirectories(destFile.getParent());

                if (Files.exists(destFile))
                    Files.delete(destFile);

                try
                {
                    Files.createLink(destFile, file);
                }
                catch (UnsupportedOperationException | IOException linkFailed)
                {
                    // Fall back to a copy if hard links are not supported (e.g.
                    // cross-device or non-POSIX filesystem).
                    Files.copy(file, destFile);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException
            {
                throw exc;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException
            {
                if (exc != null)
                    throw exc;
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Removes the entire run directory and all contents.  Called on a successful
     * run when {@code --no-cleanup} was not specified.
     *
     * <p>If the run directory does not exist, this method returns silently.
     *
     * @throws IOException if any file or directory cannot be removed
     */
    public void cleanupAll() throws IOException
    {
        if (!Files.exists(runDir))
            return;

        // Walk in reverse-depth order so children are removed before parents.
        try (var stream = Files.walk(runDir).sorted(Comparator.reverseOrder()))
        {
            for (Path p : (Iterable<Path>) stream::iterator)
            {
                try
                {
                    Files.delete(p);
                }
                catch (NoSuchFileException ignored)
                {
                    // Already gone.
                }
            }
        }
    }

    /**
     * Marker indicating that this run's data should be preserved on disk.
     *
     * <p>The current implementation is a no-op; the actual decision to invoke
     * {@link #cleanupAll()} or not is made by the caller.  This method exists so
     * that a future implementation can record a sentinel file or extend the run
     * directory's retention without changing the call sites.
     */
    public void preserve()
    {
        // No-op: callers signal preservation by simply not invoking cleanupAll().
    }
}
