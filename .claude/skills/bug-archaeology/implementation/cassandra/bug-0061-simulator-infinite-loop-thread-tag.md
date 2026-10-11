# Bug 0061: Simulator Cannot Distinguish Infinite-Loop Threads From Short-Lived Daemon Threads

**Commit**: 68aea4b15f · **Ticket**: CASSANDRA-20542
**Subsystem**: concurrent/simulator
**Category**: Logic error in condition

---

## Root Cause

The simulator relies on thread classification to decide when a simulated "Work phase" may advance: infinite-loop threads (e.g. commit-log flusher) must drain their queued work before a phase completes, while ordinary daemon threads (background bookkeeping) are not linked to phase completion. The original API expressed both concepts through a single `Daemon` enum (`DAEMON` / `NON_DAEMON`) that lived inside `InfiniteLoopExecutor`, which conflated the JVM daemon-thread flag with the simulator's phase-tracking requirement. After a rebase the intercepting executor factory in the simulator could not correctly classify newly introduced threads, causing the simulator to either deadlock waiting for daemon threads it should ignore, or to advance phases prematurely past threads it should wait for.

### Before / After

```java
// BEFORE – single enum conflates OS daemon flag with simulator semantics
public enum Daemon { DAEMON, NON_DAEMON }

Interruptible infiniteLoop(String name, Task task, SimulatorSafe simulatorSafe, Daemon daemon, Interrupts interrupts);
```

```java
// AFTER – two orthogonal enums: OS-level flag vs simulator tag
public enum SystemThreadTag  { DAEMON, NON_DAEMON }
public enum SimulatorThreadTag  { JOB, DAEMON, INFINITE_LOOP }

Interruptible infiniteLoop(String name, Task task, SimulatorSafe simulatorSafe, SystemThreadTag systemTag, Interrupts interrupts);
Thread startThread(String name, Runnable runnable, SystemThreadTag systemTag, SimulatorThreadTag simulatorTag);
```

---

## How to Spot

When a single flag/enum is overloaded to control two independent orthogonal properties (here: JVM daemon bit and simulator phase-tracking), adding a new use case that needs different values for each property has no legal representation. Review factory methods that accept mixed-concern flags and split them if they need to be independently varied.

---

## Test

`AccordHarrySimulationTest` and `HarrySimulatorTest` exercise the simulator with the new thread classification, ensuring simulation phases complete without deadlock.
