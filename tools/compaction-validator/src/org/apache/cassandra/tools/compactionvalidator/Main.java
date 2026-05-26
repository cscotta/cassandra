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
package org.apache.cassandra.tools.compactionvalidator;

import java.io.File;
import java.security.SecureRandom;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.db.Keyspace;
import org.apache.cassandra.db.commitlog.CommitLog;
import org.apache.cassandra.dht.Murmur3Partitioner;
import org.apache.cassandra.io.sstable.format.big.BigFormat;
import org.apache.cassandra.tcm.ClusterMetadataService;
import org.apache.cassandra.tools.compactionvalidator.config.ConfigParser;
import org.apache.cassandra.tools.compactionvalidator.config.RunConfig;
import org.apache.cassandra.tools.compactionvalidator.logging.RunLogger;
import org.apache.cassandra.tools.compactionvalidator.util.SeedUtil;

@Command(
    name = "compaction-validator",
    description = "Run an A/B compaction-output validator. Generates a deterministic "
                  + "data set per seed, compacts it twice using two configurations "
                  + "(control + experiment), and verifies the two outputs are "
                  + "byte-/cell-identical. See tools/compaction-validator/configs/sample.yaml.",
    mixinStandardHelpOptions = true
)
public class Main implements Runnable
{
    @Option(names = { "--config", "-c" }, required = true,
            description = "Path to the YAML config file (required). Defines the comparison's "
                          + "control and experiment sides plus run-loop knobs. See "
                          + "tools/compaction-validator/configs/sample.yaml.")
    File configFile;

    @Option(names = { "--seed", "-s" }, description = "Root RNG seed (decimal or 0x-prefixed hex). "
                                                       + "Default: random per run. Override the YAML / random seed for repro.",
            converter = SeedConverter.class)
    Long seed = null;

    public static class SeedConverter implements picocli.CommandLine.ITypeConverter<Long>
    {
        @Override
        public Long convert(String value) throws Exception
        {
            String trimmed = value.trim();
            if (trimmed.startsWith("0x") || trimmed.startsWith("0X"))
                return Long.parseUnsignedLong(trimmed.substring(2), 16);
            return Long.parseLong(trimmed);
        }
    }

    @Option(names = { "--working-dir", "-w" }, description = "Working directory base for SSTable / commitlog state. "
                                                              + "Per-run subdirectories are created under this path.")
    File workingDir = new File(System.getProperty("java.io.tmpdir"), "cassandra-compaction-validator");

    @Option(names = { "--log-file", "-l" }, description = "Append-only log file. Default: ./compaction-validator.log")
    File logFile = new File("compaction-validator.log");

    @Option(names = { "--once" }, description = "Run a single iteration then exit (overrides run.max_runs in the config).")
    boolean runOnce = false;

    /** Loaded once at startup; non-null for the rest of the process lifetime. */
    RunConfig config;

    public static void main(String[] args)
    {
        bootstrapJvm();
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run()
    {
        // Load + validate the config first so any malformed YAML fails the run before
        // we touch the filesystem or kick off the orchestrator.
        try
        {
            config = ConfigParser.load(configFile);
        }
        catch (Exception e)
        {
            System.err.println("Failed to load --config " + configFile + ": " + e.getMessage());
            System.exit(2);
            return;
        }

        if (seed == null)
            seed = new SecureRandom().nextLong();

        System.out.printf("CompactionValidator starting: seed=%s workingDir=%s%n",
                          SeedUtil.toHex(seed), workingDir);
        System.out.printf("  control=%s  experiment=%s%n",
                          config.comparison.controlName(), config.comparison.experimentName());

        ProgressTap reporter = createProgressTap();

        try
        {
            if (!workingDir.exists() && !workingDir.mkdirs())
                throw new RuntimeException("Could not create working directory: " + workingDir);

            try (RunLogger runLogger = new RunLogger(logFile))
            {
                runLogger.writeHeader();

                RunLoop loop = new RunLoop(this, runLogger, reporter);
                RunResult finalResult = loop.run();

                if (finalResult != null && finalResult.isFailure())
                    System.exit(1);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.exit(1);
        }
        finally
        {
            if (reporter instanceof AutoCloseable)
            {
                try
                {
                    ((AutoCloseable) reporter).close();
                }
                catch (Exception ignored)
                {
                }
            }
        }
    }

    /**
     * Builds the progress reporter.  Defaults to the Lanterna TUI; falls
     * back to a plain-text reporter if the config asked for {@code no_ui}
     * or if the TUI cannot open the terminal (e.g. no controlling tty).
     */
    ProgressTap createProgressTap()
    {
        if (config != null && config.run != null && config.run.noUi)
            return new PlainTextReporter(System.out);

        try
        {
            org.apache.cassandra.tools.compactionvalidator.tui.TuiManager tui =
                new org.apache.cassandra.tools.compactionvalidator.tui.TuiManager();
            tui.start();
            return tui;
        }
        catch (Throwable t)
        {
            System.err.println("Warning: TUI unavailable (" + t.getMessage() + "), falling back to plain text");
            return new PlainTextReporter(System.out);
        }
    }

    public static void bootstrapJvm()
    {
        System.out.println("Initializing Cassandra daemon...");
        DatabaseDescriptor.daemonInitialization();
        // ClusterMetadataService.initializeForTools below reads system_schema to seed an
        // empty TCM state — that path goes through SchemaKeyspace.fetchNonSystemKeyspaces
        // which expects either committed SSTables or a live commitlog to back the system
        // tables. With both off this fails on first launch. Keeping commitlog start; with
        // cassandra.storagedir now anchored under --working-dir/_cassandra-system (wiped at
        // launch by the launcher script), the commitlog directory starts empty every launch
        // and no segments leak cross-launch.
        System.out.println("Initializing Commitlog...");
        CommitLog.instance.start();
        System.out.println("Initializing Cluster Metadata Service...");
        // false: don't load any pre-existing system_schema SSTables. We start fresh on every
        // launch — there's nothing meaningful to load — and a stale transaction log left over
        // from a prior crash would cause "inconsistent disk state" errors here. With false the
        // CMS scans no SSTables and starts in seconds rather than minutes.
        ClusterMetadataService.initializeForTools(false);
        System.out.println("Initializing keyspace...");
        Keyspace.setInitialized();
        // Cursor compaction only supports BigFormat
        DatabaseDescriptor.setSelectedSSTableFormat(BigFormat.getInstance());
        // CursorCompactor.isSupported requires a partitioner with supportsReusableKeys()
        DatabaseDescriptor.setPartitionerUnsafe(Murmur3Partitioner.instance);
        // Disable compaction-throughput throttling. Default is 64 MiB/s, which would silently
        // cap our measured throughput well below what either backend can actually do and skew
        // the legacy-vs-cursor speedup comparison. Cassandra's CompactionManager.setRateInBytes
        // treats 0 as "unlimited" (sets the internal rate limiter to Double.MAX_VALUE), and
        // CompactionManager.getRateLimiter() pulls fresh from this DatabaseDescriptor value
        // on every call, so setting it here is sufficient — no need to also poke the manager.
        DatabaseDescriptor.setCompactionThroughputBytesPerSec(0);
    }
}
