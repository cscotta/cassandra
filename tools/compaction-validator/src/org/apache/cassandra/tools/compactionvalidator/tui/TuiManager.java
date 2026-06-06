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
package org.apache.cassandra.tools.compactionvalidator.tui;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;

import org.apache.cassandra.tools.compactionvalidator.ProgressTap;
import org.apache.cassandra.tools.compactionvalidator.RunResult;
import org.apache.cassandra.tools.compactionvalidator.SstableInfo;
import org.apache.cassandra.tools.compactionvalidator.compaction.CompactionStats;
import org.apache.cassandra.tools.compactionvalidator.datagen.DataGenStats;
import org.apache.cassandra.tools.compactionvalidator.schema.GeneratedSchema;
import org.apache.cassandra.tools.compactionvalidator.validation.MismatchReport;
import org.apache.cassandra.tools.compactionvalidator.validation.ValidationStats;

/**
 * Top-level driver for the Lanterna TUI.  Implements {@link ProgressTap} so
 * the run orchestrator can publish events to it; internally translates each
 * tap call into a {@link TuiEvent} on the {@link ProgressBus}, drained by a
 * dedicated render thread at ~30 fps.
 *
 * <p>The renderer is resilient to terminal resize and to terminals smaller
 * than the preferred (80 × 30) layout: panels are scaled down or hidden
 * proportionally; if the terminal is unusably small, the screen shows a
 * "Terminal too small" message.
 */
public class TuiManager implements ProgressTap, AutoCloseable
{
    /**
     * Render rate. 1 Hz keeps the screen calm — every panel still updates every event,
     * because events update local state immediately and the next render frame draws the
     * latest state. Higher rates caused visible flicker on most terminals because of the
     * pre-frame full-screen clear; with that removed (Lanterna's TerminalScreen does
     * a diff-only refresh anyway) the only reason to go higher is animation smoothness.
     */
    private static final int TARGET_FPS = 1;
    private static final int MIN_COLUMNS = 60;
    private static final int MIN_ROWS = 12;

    private final Terminal terminal;
    private final TerminalScreen screen;
    private final ProgressBus bus = new ProgressBus();

    private final HeaderPanel header = new HeaderPanel();
    private final SchemaPanel schema = new SchemaPanel();
    private final DataGenPanel dataGen = new DataGenPanel();
    private final CompactionPanel compaction = new CompactionPanel();
    private final ValidationPanel validation = new ValidationPanel();
    private final RunHistoryPanel history = new RunHistoryPanel();
    private final SummaryPanel summary = new SummaryPanel();

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean quitRequested = new AtomicBoolean(false);
    private Thread renderThread;

    /** @return {@code true} if the user pressed 'q' or Ctrl-C / Esc since the TUI started. */
    public boolean isQuitRequested()
    {
        return quitRequested.get();
    }

    /**
     * Builds the TUI.  Allocates a Lanterna terminal and a screen but does
     * not start the render loop; call {@link #start()} for that.
     *
     * @throws IOException if the terminal cannot be opened
     */
    public TuiManager() throws IOException
    {
        DefaultTerminalFactory factory = new DefaultTerminalFactory();
        this.terminal = factory.createTerminal();
        this.screen = new TerminalScreen(this.terminal);
        this.screen.setCursorPosition(null); // hide cursor
    }

    /** Starts the screen and the render thread.  Idempotent. */
    public synchronized void start()
    {
        if (running.compareAndSet(false, true))
        {
            try
            {
                screen.startScreen();
            }
            catch (IOException e)
            {
                running.set(false);
                throw new RuntimeException("Failed to start screen: " + e.getMessage(), e);
            }
            renderThread = new Thread(this::renderLoop, "compaction-validator-tui");
            renderThread.setDaemon(true);
            renderThread.start();
        }
    }

    /**
     * Stops the render thread and closes the screen.  Safe to call multiple
     * times; safe to call from any thread.
     */
    public synchronized void stop()
    {
        if (running.compareAndSet(true, false))
        {
            if (renderThread != null)
            {
                try
                {
                    renderThread.join(1000);
                }
                catch (InterruptedException ie)
                {
                    Thread.currentThread().interrupt();
                }
            }
            try
            {
                screen.stopScreen();
            }
            catch (IOException ignored)
            {
            }
            try
            {
                terminal.close();
            }
            catch (IOException ignored)
            {
            }
        }
    }

    @Override
    public void close()
    {
        stop();
    }

    /**
     * Blocks until the user presses any key.  Used after a failed run to
     * give the operator a chance to read the failure detail before the loop
     * continues.
     *
     * @return the character pressed, or {@code 0} if a non-character key
     */
    public char waitForKey()
    {
        try
        {
            while (running.get())
            {
                KeyStroke ks = terminal.pollInput();
                if (ks != null)
                {
                    Character ch = ks.getCharacter();
                    if (ks.getKeyType() == KeyType.Escape)
                        return (char) 27;
                    if (ks.getKeyType() == KeyType.Enter)
                        return '\n';
                    return ch != null ? ch.charValue() : (char) 0;
                }
                Thread.sleep(50);
            }
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
        catch (IOException ignored)
        {
        }
        return (char) 0;
    }

    // -------------------------------------------------------------------------
    // ProgressTap
    // -------------------------------------------------------------------------

    @Override
    public void onRunStart(int runNumber, long seed)
    {
        bus.push(new TuiEvent.RunStart(runNumber, seed));
    }

    @Override
    public void onComparisonLabels(String controlName, String experimentName)
    {
        bus.push(new TuiEvent.ComparisonLabels(controlName, experimentName));
    }

    @Override
    public void onSchemaReady(GeneratedSchema schema)
    {
        bus.push(new TuiEvent.SchemaReady(schema));
    }

    @Override
    public void onDataGenProgress(long bytes, long target, long partitions, long rows, double bytesPerSec)
    {
        bus.push(new TuiEvent.DataGenProgress(bytes, target, partitions, rows, bytesPerSec));
    }

    @Override
    public void onDataGenComplete(DataGenStats stats, long durationMs)
    {
        bus.push(new TuiEvent.DataGenComplete(stats, durationMs));
    }

    @Override
    public void onCompactionProgress(String backend, long bytes, long target, long partitions, double bytesPerSec,
                                     long currentTaskBytes, long currentTaskTotal)
    {
        bus.push(new TuiEvent.CompactionProgress(backend, bytes, target, partitions, bytesPerSec,
                                                 currentTaskBytes, currentTaskTotal));
    }

    @Override
    public void onSstableEmitted(String backend, String filename, long sizeBytes, int level)
    {
        bus.push(new TuiEvent.SstableEmitted(backend, filename, sizeBytes, level));
    }

    @Override
    public void onCompactionInputs(String backend,
                                   List<SstableInfo> inputs)
    {
        bus.push(new TuiEvent.CompactionInputs(backend, inputs));
    }

    @Override
    public void onCompactionTaskStart(String backend,
                                      List<SstableInfo> taskInputs,
                                      int targetLevel,
                                      int taskIndex,
                                      int tasksInBatch,
                                      int totalTasksDone)
    {
        bus.push(new TuiEvent.CompactionTaskStart(backend, taskInputs, targetLevel,
                                                  taskIndex, tasksInBatch, totalTasksDone));
    }

    @Override
    public void onCompactionTaskEnd(String backend,
                                    int taskIndex,
                                    List<SstableInfo> taskOutputs)
    {
        bus.push(new TuiEvent.CompactionTaskEnd(backend, taskIndex, taskOutputs));
    }

    @Override
    public void onCompactionStatus(String backend, String message)
    {
        bus.push(new TuiEvent.CompactionStatus(backend, message));
    }

    @Override
    public void onCompactionComplete(String backend, CompactionStats stats)
    {
        bus.push(new TuiEvent.CompactionComplete(backend, stats));
    }

    @Override
    public void onValidationProgress(long partitionsChecked)
    {
        bus.push(new TuiEvent.ValidationProgress(partitionsChecked));
    }

    @Override
    public void onValidationComplete(ValidationStats stats, boolean success)
    {
        bus.push(new TuiEvent.ValidationComplete(stats, success));
    }

    @Override
    public void onRunComplete(RunResult result)
    {
        bus.push(new TuiEvent.RunComplete(result));
    }

    @Override
    public void onRunFailed(MismatchReport report, RunResult result)
    {
        bus.push(new TuiEvent.RunFailed(report, result));
    }

    // -------------------------------------------------------------------------
    // Render loop
    // -------------------------------------------------------------------------

    private void renderLoop()
    {
        long frameNanos = 1_000_000_000L / TARGET_FPS;
        while (running.get())
        {
            long start = System.nanoTime();
            try
            {
                renderFrame();
            }
            catch (IOException e)
            {
                // Best-effort: try again next frame
            }
            catch (RuntimeException re)
            {
                // Don't let a render bug kill the thread silently.
                // Print to stderr so the operator can see it after the screen exits.
                System.err.println("TUI render error: " + re);
            }
            long elapsed = System.nanoTime() - start;
            long sleep = frameNanos - elapsed;
            if (sleep > 0)
            {
                try
                {
                    Thread.sleep(sleep / 1_000_000L, (int) (sleep % 1_000_000L));
                }
                catch (InterruptedException ie)
                {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private void renderFrame() throws IOException
    {
        TerminalSize newSize = screen.doResizeIfNecessary();
        if (newSize == null)
            newSize = screen.getTerminalSize();

        // Drain events and dispatch
        List<TuiEvent> events = bus.drain();
        for (TuiEvent ev : events)
            dispatch(ev);

        // Poll keystrokes and honour 'q' / Esc / Ctrl-C as a quit request.
        // Drain everything queued so the terminal doesn't accumulate input.
        for (KeyStroke ks; (ks = terminal.pollInput()) != null; )
        {
            if (ks.getKeyType() == KeyType.EOF)
            {
                quitRequested.set(true);
                break;
            }
            if (ks.getKeyType() == KeyType.Escape)
            {
                quitRequested.set(true);
                break;
            }
            // While the summary is showing a failure, route navigation keys to its
            // scroll API so the user can read the full inspection view (which is
            // typically much taller than the screen).
            if (summary.isActive() && summary.isFailure())
            {
                KeyType kt = ks.getKeyType();
                if (kt == KeyType.ArrowUp)   { summary.scrollUp(1);   continue; }
                if (kt == KeyType.ArrowDown) { summary.scrollDown(1); continue; }
                if (kt == KeyType.PageUp)    { summary.scrollUp(10);  continue; }
                if (kt == KeyType.PageDown)  { summary.scrollDown(10); continue; }
                if (kt == KeyType.Home)      { summary.scrollHome();  continue; }
                if (kt == KeyType.End)       { summary.scrollEnd();   continue; }
            }
            Character ch = ks.getCharacter();
            if (ch != null)
            {
                char c = Character.toLowerCase(ch);
                if (c == 'q')
                {
                    quitRequested.set(true);
                    break;
                }
                // Ctrl-C — most terminals deliver this as character 0x03 when raw mode is on.
                if (c == 0x03 && (ks.isCtrlDown() || ch == 0x03))
                {
                    quitRequested.set(true);
                    break;
                }
            }
        }

        // Wipe the entire back-buffer to spaces before drawing this frame. Without this,
        // when panel positions shift between frames (e.g. the schema CQL has fewer lines
        // for run N+1 than run N, so every panel below it moves up), characters drawn at
        // the OLD position of a panel survive in the back-buffer and re-appear on screen.
        // The user-visible symptom of that bug is the validation bar appearing twice
        // immediately after a new run starts: the old "100% complete" rendering still
        // sits at the prior y-position, and the new run's empty bar paints in below it.
        //
        // Lanterna's TerminalScreen.refresh() only sends cells that differ between the
        // back-buffer and the terminal-side buffer, so filling the back-buffer to spaces
        // first does NOT cause flicker — cells whose final state matches the previous
        // frame produce no diff, and cells that genuinely changed are repainted exactly
        // once. Each panel's render() then paints over the spaces with its own content.
        TextGraphics g = screen.newTextGraphics();
        g.setBackgroundColor(TextColor.ANSI.DEFAULT);
        g.setForegroundColor(TextColor.ANSI.DEFAULT);
        g.fillRectangle(new TerminalPosition(0, 0), newSize, ' ');
        renderAll(g, newSize);
        screen.refresh();
    }

    private void dispatch(TuiEvent ev)
    {
        header.onEvent(ev);
        schema.onEvent(ev);
        dataGen.onEvent(ev);
        compaction.onEvent(ev);
        validation.onEvent(ev);
        history.onEvent(ev);
        summary.onEvent(ev);
    }

    private void renderAll(TextGraphics g, TerminalSize size)
    {
        int width = size.getColumns();
        int rows = size.getRows();

        // If terminal is unusably small, show a notice
        if (width < MIN_COLUMNS || rows < MIN_ROWS)
        {
            renderTooSmall(g, size);
            return;
        }

        // If summary is active, draw it on top of (instead of) the rest
        if (summary.isActive())
        {
            // Header first (so the user can see the run number/seed)
            int headerRow = 0;
            int headerHeight = 1;
            header.render(g, new TerminalPosition(0, headerRow), new TerminalSize(width, headerHeight));

            // Summary fills the rest of the screen up to a footer
            int footerHeight = 1;
            int summaryHeight = Math.max(1, rows - headerHeight - footerHeight);
            summary.render(g, new TerminalPosition(0, headerHeight),
                           new TerminalSize(width, summaryHeight));

            renderFooter(g, size);
            return;
        }

        // Standard layout
        Layout layout = computeLayout(width, rows);

        if (layout.headerHeight > 0)
            header.render(g, new TerminalPosition(0, layout.headerY),
                          new TerminalSize(width, layout.headerHeight));

        if (layout.schemaHeight > 0)
            schema.render(g, new TerminalPosition(0, layout.schemaY),
                          new TerminalSize(width, layout.schemaHeight));

        if (layout.dataGenHeight > 0)
            dataGen.render(g, new TerminalPosition(0, layout.dataGenY),
                           new TerminalSize(width, layout.dataGenHeight));

        if (layout.compactionHeight > 0)
            compaction.render(g, new TerminalPosition(0, layout.compactionY),
                              new TerminalSize(width, layout.compactionHeight));

        if (layout.validationHeight > 0)
            validation.render(g, new TerminalPosition(0, layout.validationY),
                              new TerminalSize(width, layout.validationHeight));

        if (layout.historyHeight > 0)
            history.render(g, new TerminalPosition(0, layout.historyY),
                           new TerminalSize(width, layout.historyHeight));

        renderFooter(g, size);
    }

    private void renderFooter(TextGraphics g, TerminalSize size)
    {
        int width = size.getColumns();
        int rows = size.getRows();
        if (rows < 1)
            return;
        int row = rows - 1;
        g.setBackgroundColor(TextColor.ANSI.BLACK_BRIGHT);
        g.setForegroundColor(TextColor.ANSI.WHITE);
        g.fillRectangle(new TerminalPosition(0, row), new TerminalSize(width, 1), ' ');
        String hint = " q=quit  ";
        if (summary.isActive() && summary.isFailure())
            hint = " q=quit  r=retry  n=next-seed  ";
        g.putString(0, row, TuiUtil.truncate(hint, width));

        // Right-side heap-usage indicator: Heap: U/M GB (P%)
        // Uses Runtime.totalMemory() (currently committed) - freeMemory() (free in committed) for
        // "used", and maxMemory() for the -Xmx ceiling. Shown in GiB to two decimal places so
        // small allocations don't swamp the trailing digit.
        Runtime rt = Runtime.getRuntime();
        long usedBytes = rt.totalMemory() - rt.freeMemory();
        long maxBytes = rt.maxMemory();
        double usedGb = usedBytes / (1024.0 * 1024.0 * 1024.0);
        double maxGb = maxBytes / (1024.0 * 1024.0 * 1024.0);
        int pct = maxBytes > 0 ? (int) ((usedBytes * 100L) / maxBytes) : 0;
        String heapStr = String.format("Heap: %.2f/%.2f GB (%d%%) ", usedGb, maxGb, pct);
        int x = width - heapStr.length();
        if (x > hint.length())
        {
            // Color the indicator yellow at >70% usage and red at >90% so it stands out
            // without being noisy when the JVM is comfortable.
            TextColor heapColor = TextColor.ANSI.BLACK_BRIGHT;
            if (pct >= 90) heapColor = TextColor.ANSI.RED;
            else if (pct >= 70) heapColor = TextColor.ANSI.YELLOW;
            g.setForegroundColor(heapColor);
            g.putString(x, row, heapStr);
        }
        // Reset
        g.setBackgroundColor(TextColor.ANSI.DEFAULT);
        g.setForegroundColor(TextColor.ANSI.DEFAULT);
    }

    private void renderTooSmall(TextGraphics g, TerminalSize size)
    {
        g.setBackgroundColor(TextColor.ANSI.DEFAULT);
        g.setForegroundColor(TextColor.ANSI.YELLOW);
        int rows = size.getRows();
        int cols = size.getColumns();
        if (rows < 1 || cols < 1)
            return;
        String msg = String.format("Terminal too small (%dx%d, need %dx%d)",
                                   cols, rows, MIN_COLUMNS, MIN_ROWS);
        int row = rows / 2;
        int col = Math.max(0, (cols - msg.length()) / 2);
        g.putString(col, row, TuiUtil.truncate(msg, cols), SGR.BOLD);
    }

    /**
     * Layout descriptor.  Computed once per frame from the current terminal
     * size.  When space is short, panels are dropped in this order: history,
     * schema, validation, data-gen, compaction (compaction is the most important
     * and is the last to be hidden).
     */
    private static final class Layout
    {
        int headerY, headerHeight;
        int schemaY, schemaHeight;
        int dataGenY, dataGenHeight;
        int compactionY, compactionHeight;
        int validationY, validationHeight;
        int historyY, historyHeight;
    }

    private Layout computeLayout(int width, int rows)
    {
        Layout layout = new Layout();
        layout.headerHeight = 1;

        // 1-row visual breathing room between each pair of adjacent visible
        // panels. Up to 5 potential gaps (header→schema, schema→dataGen,
        // dataGen→compaction, compaction→validation, validation→history); when
        // schema or history end up at height 0 the gap before them is skipped
        // by the Y-coordinate pass below. We reserve the worst case here so the
        // sizing pass has a stable budget — any unused gap rows become a small
        // strip of empty space above the footer, which is harmless.
        final int PANEL_GAP = 1;
        final int worstCaseGaps = 5;

        // Reserve 1 row for the bottom footer
        int available = Math.max(0, rows - layout.headerHeight - 1 - worstCaseGaps);

        // Preferred sizes
        // Schema panel preferred height is dynamic: it asks for as many lines as the
        // CQL has (plus one for the title), capped at half the terminal so it can't
        // squeeze the more important panels off-screen. Fall back to 8 when the
        // schema panel hasn't been populated yet (first frames before SchemaReady).
        int schemaDesired = schema.getDesiredHeight();
        int prefSchema = schemaDesired > 0 ? schemaDesired : 8;
        prefSchema = Math.min(prefSchema, Math.max(8, rows / 2));
        int prefDataGen = 4;
        int prefCompaction = 10;
        int prefValidation = 3;
        // History panel: 1 header row + up to 5 entries fits comfortably in 6 rows on tall
        // terminals, but we'll shrink as needed when space is tight.
        int prefHistory = 6;

        int total = prefSchema + prefDataGen + prefCompaction + prefValidation + prefHistory;

        if (available >= total)
        {
            // Plenty of room
            layout.schemaHeight = prefSchema;
            layout.dataGenHeight = prefDataGen;
            layout.compactionHeight = prefCompaction;
            layout.validationHeight = prefValidation;
            layout.historyHeight = prefHistory;
        }
        else
        {
            // Drop history first, then schema, then shrink the rest.
            layout.historyHeight = 0;
            int leftover = available - (prefSchema + prefDataGen + prefCompaction + prefValidation);
            if (leftover >= 2)
            {
                layout.historyHeight = Math.min(prefHistory, leftover);
                leftover -= layout.historyHeight;
            }
            layout.schemaHeight = prefSchema;
            int dataGen = prefDataGen;
            int compaction = prefCompaction;
            int valid = prefValidation;
            int sum = layout.schemaHeight + dataGen + compaction + valid + layout.historyHeight;
            // First, shrink schema if needed
            while (sum > available && layout.schemaHeight > 0)
            {
                layout.schemaHeight--;
                sum--;
            }
            // Then shrink data-gen to 1
            while (sum > available && dataGen > 1)
            {
                dataGen--;
                sum--;
            }
            // Then shrink validation to 1
            while (sum > available && valid > 1)
            {
                valid--;
                sum--;
            }
            // Then shrink compaction (but keep ≥ 4 if possible)
            while (sum > available && compaction > 4)
            {
                compaction--;
                sum--;
            }
            // Then shrink history to 0 if not already
            while (sum > available && layout.historyHeight > 0)
            {
                layout.historyHeight--;
                sum--;
            }
            // Last resort: shrink compaction to 1
            while (sum > available && compaction > 1)
            {
                compaction--;
                sum--;
            }
            layout.dataGenHeight = dataGen;
            layout.compactionHeight = compaction;
            layout.validationHeight = valid;
        }

        // Y coordinates (header at row 0). A {@link #PANEL_GAP} blank row is inserted
        // before each subsequent panel that has nonzero height — so visible panels are
        // visually separated, but hidden panels don't waste a leading gap row.
        layout.headerY = 0;
        int nextY = layout.headerY + layout.headerHeight;
        boolean prevVisible = layout.headerHeight > 0;

        if (layout.schemaHeight > 0)
        {
            if (prevVisible) nextY += PANEL_GAP;
            layout.schemaY = nextY;
            nextY += layout.schemaHeight;
            prevVisible = true;
        }
        if (layout.dataGenHeight > 0)
        {
            if (prevVisible) nextY += PANEL_GAP;
            layout.dataGenY = nextY;
            nextY += layout.dataGenHeight;
            prevVisible = true;
        }
        if (layout.compactionHeight > 0)
        {
            if (prevVisible) nextY += PANEL_GAP;
            layout.compactionY = nextY;
            nextY += layout.compactionHeight;
            prevVisible = true;
        }
        if (layout.validationHeight > 0)
        {
            if (prevVisible) nextY += PANEL_GAP;
            layout.validationY = nextY;
            nextY += layout.validationHeight;
            prevVisible = true;
        }
        if (layout.historyHeight > 0)
        {
            if (prevVisible) nextY += PANEL_GAP;
            layout.historyY = nextY;
            nextY += layout.historyHeight;
        }

        return layout;
    }

    // -------------------------------------------------------------------------
    // Accessors used by tests
    // -------------------------------------------------------------------------

    /** @return the underlying screen (test/diagnostic use only) */
    public Screen getScreen()
    {
        return screen;
    }

    /** @return whether the render thread is currently active */
    public boolean isRunning()
    {
        return running.get();
    }
}
