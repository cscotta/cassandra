## Commit

391b604c97 — KAFKA-19226: Add ConsoleShareConsumer system test

## Ticket

KAFKA-19226

## Subsystem

Share groups / system tests

## Category

Missing null / bounds check

## Root cause

The `ConsoleShareConsumer` command-line tool lacked any end-to-end system test. Without a sanity-check test, regressions in consumer startup, message consumption, or graceful shutdown would go undetected until manual integration testing. The missing test class meant that CI could pass even if the console consumer failed to connect to a share group or crashed on the first poll.

## Before / After

```python
# Before — no system test for ConsoleShareConsumer
# (no test file existed)

# After — new test verifying lifecycle
class TestConsoleShareConsumer(Test):
    def test_simple_consume(self, ...):
        consumer = ConsoleShareConsumer(self.test_context, ...)
        consumer.start()
        wait_until(lambda: consumer.total_consumed() >= num_messages, ...)
        consumer.stop()
```

## How to spot

Search the system test suite for coverage of each console-tool class. Any `Console*Consumer` or `Console*Producer` that lacks a corresponding `test_console_*.py` file is a gap where lifecycle regressions will be invisible to CI.

## Test

New `test_console_share_consumer.py` and `console_share_consumer.py` service files cover start, consume, and stop lifecycle for the ConsoleShareConsumer.
