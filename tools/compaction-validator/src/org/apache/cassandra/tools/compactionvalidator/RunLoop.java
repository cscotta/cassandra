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

import java.security.SecureRandom;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cassandra.tools.compactionvalidator.config.RunConfig;
import org.apache.cassandra.tools.compactionvalidator.logging.RunLogger;
import org.apache.cassandra.tools.compactionvalidator.tui.TuiManager;
import org.apache.cassandra.tools.compactionvalidator.util.ByteUtil;
import org.apache.cassandra.tools.compactionvalidator.util.SeedUtil;
import org.apache.cassandra.tools.compactionvalidator.validation.ErrataRule;

/**
 * Long-running outer loop that repeatedly invokes a {@link RunOrchestrator}.
 *
 * <p>Each iteration generates a fresh seed (or uses the user-supplied seed for
 * the first iteration), drives one full validation run, appends the result to
 * the {@link RunLogger}, and propagates progress events to the
 * {@link ProgressTap}.
 *
 * <p>A {@link Runtime#addShutdownHook} is registered so SIGTERM and Ctrl-C
 * cause the loop to exit cleanly after the in-flight run finishes its current
 * phase boundary.  The shutdown flag is checked between runs (not in the middle
 * of one) so the validator never leaves a half-finished SSTable behind.
 *
 * <p>On a validation failure, the loop stops immediately: the failed run's
 * directory is preserved, the failure is logged, and {@link #run()} returns.
 * The caller (typically {@link Main}) is expected to {@code exit(1)} from there.
 */
public class RunLoop
{
    private static final Logger logger = LoggerFactory.getLogger(RunLoop.class);

    /** Pause between successive successful runs (gives the user a chance to read the summary). */
    private static final long INTER_RUN_PAUSE_MS = TimeUnit.SECONDS.toMillis(7);

    private final Main config;
    private final RunLogger runLogger;
    private final ProgressTap reporter;
    private final Thread shutdownHook;

    private volatile boolean running = true;

    /**
     * @param config    parsed CLI options (read-only; never mutated)
     * @param runLogger append-only structured log writer
     * @param reporter  progress callback (plain-text or TUI-backed)
     */
    public RunLoop(Main config, RunLogger runLogger, ProgressTap reporter)
    {
        if (config == null)
            throw new IllegalArgumentException("config must not be null");
        if (runLogger == null)
            throw new IllegalArgumentException("runLogger must not be null");
        if (reporter == null)
            throw new IllegalArgumentException("reporter must not be null");

        this.config = config;
        this.runLogger = runLogger;
        this.reporter = reporter;
        this.shutdownHook = new Thread(this::shutdown, "compaction-validator-shutdown");
    }

    /**
     * Runs the outer loop until one of the following happens:
     * <ul>
     *   <li>{@code --once} is set and the first run completes</li>
     *   <li>{@code --max-runs N} is set and N runs have completed</li>
     *   <li>A run fails (mismatch or exception); the failure is reported and the loop exits</li>
     *   <li>{@link #shutdown()} is invoked by a shutdown hook</li>
     * </ul>
     *
     * @return the final {@link RunResult}, or {@code null} if no runs were started
     * @throws Exception if logging or progress reporting throws (run-level errors are
     *                   captured inside {@link RunOrchestrator#execute()})
     */
    public RunResult run() throws Exception
    {
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        long currentSeed = config.seed != null ? config.seed : new SecureRandom().nextLong();
        int runNumber = 0;
        RunResult lastResult = null;

        // Resolve everything we need from RunConfig once. The CLI Main object holds
        // environment-level overrides (seed/workingDir/logFile/runOnce); the YAML
        // RunConfig holds everything else. Errata and target bytes get parsed up
        // front so a malformed value crashes the run before any I/O.
        RunConfig cfg = config.config;
        long targetBytes;
        try
        {
            targetBytes = ByteUtil.parseBytes(cfg.run.targetBytes);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Invalid run.target_bytes '" + cfg.run.targetBytes + "': " + e.getMessage(), e);
        }
        Set<ErrataRule> activeErrata =
            ErrataRule.parseCliList(
                cfg.run.ignoreErrata == null ? "" : String.join(",", cfg.run.ignoreErrata));

        try
        {
            while (running)
            {
                runNumber++;

                logger.info("Starting run #{} with seed {}", runNumber, SeedUtil.toHex(currentSeed));

                RunOrchestrator orchestrator = new RunOrchestrator(currentSeed,
                                                                   runNumber,
                                                                   config.workingDir,
                                                                   targetBytes,
                                                                   cfg.run.datagenThreads,
                                                                   cfg.run.compactionThreads,
                                                                   cfg.run.validationThreads,
                                                                   activeErrata,
                                                                   /* cleanupOnSuccess = */ !cfg.run.noCleanup,
                                                                   reporter,
                                                                   cfg);

                RunResult result = orchestrator.execute();
                lastResult = result;
                runLogger.appendRunEntry(result);

                if (result.isFailure())
                {
                    logger.error("Run #{} failed: {}", runNumber,
                                 result.failureDetail != null ? result.failureDetail : "(no detail)");
                    return result;
                }

                if (config.runOnce)
                {
                    logger.info("--once specified; exiting after run #{}", runNumber);
                    return result;
                }

                if (cfg.run.maxRuns > 0 && runNumber >= cfg.run.maxRuns)
                {
                    logger.info("run.max_runs {} reached; exiting", cfg.run.maxRuns);
                    return result;
                }

                // Inter-run pause; interruptible so shutdown hooks can break the wait.
                if (running)
                {
                    long deadline = System.currentTimeMillis() + INTER_RUN_PAUSE_MS;
                    while (running && System.currentTimeMillis() < deadline)
                    {
                        if (isQuitRequested())
                        {
                            logger.info("Quit requested via TUI; exiting after run #{}", runNumber);
                            return result;
                        }
                        try
                        {
                            Thread.sleep(Math.min(200L, deadline - System.currentTimeMillis()));
                        }
                        catch (InterruptedException ie)
                        {
                            Thread.currentThread().interrupt();
                            running = false;
                        }
                    }
                }

                if (isQuitRequested())
                {
                    logger.info("Quit requested via TUI; exiting after run #{}", runNumber);
                    return result;
                }

                currentSeed = RunSeed.nextSeed(currentSeed, runNumber);
            }
        }
        finally
        {
            // Avoid IllegalStateException if the JVM is already shutting down.
            try
            {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            }
            catch (IllegalStateException ignored)
            {
                // JVM already in shutdown; nothing to remove.
            }
        }
        return lastResult;
    }

    /**
     * Asks the loop to stop after the in-flight run completes.  Safe to call
     * from any thread, and idempotent.
     */
    public void shutdown()
    {
        running = false;
    }

    /** @return {@code true} if the loop is still running (test-only accessor) */
    public boolean isRunning()
    {
        return running;
    }

    /**
     * Returns {@code true} when the user has requested a quit via the TUI (pressing 'q' or Esc).
     * The loop checks this between runs and during the inter-run sleep so it can exit promptly.
     */
    private boolean isQuitRequested()
    {
        return reporter instanceof TuiManager tui
               && tui.isQuitRequested();
    }
}
