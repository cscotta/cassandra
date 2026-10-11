# Bug 0455: Unicode Characters Cause Encoding Errors in cqlsh Input

**Commit**: 8abbfc1fc5 · **Ticket**: CASSANDRA-15990
**Subsystem**: `bin/cqlsh.py`
**Category**: Wrong serialization / deserialization

---

## Root Cause

On Python 3, command-line option values (username, password, keyspace, execute) returned by `optparse` are native `str` objects but certain code paths — including the `cmd.Cmd` prompt machinery — expected them to be Unicode text that had been validated by `ensure_text`. Without that normalization, passing a string containing non-ASCII characters (e.g., a Unicode username or a `--execute` argument with non-ASCII content) could cause a `UnicodeDecodeError` or `TypeError` deep inside the readline/input path. Similarly, `CQLSH_PROMPT` was read raw from the environment and compared as bytes on some platforms, and the `input()` call received a prompt that had not been coerced to a proper string type.

### Before / After

```python
# BEFORE
custom_prompt = os.getenv('CQLSH_PROMPT', '')
...
def get_input_line(self, prompt=''):
    if self.tty:
        self.lastcmd = input(prompt)
```

```python
# AFTER
custom_prompt = ensure_text(os.getenv('CQLSH_PROMPT', ''))
...
def get_input_line(self, prompt=''):
    if self.tty:
        self.lastcmd = input(ensure_str(prompt))

# In read_options, after parse_args:
options.execute  = maybe_ensure_text(options.execute)
options.username = maybe_ensure_text(options.username)
options.password = maybe_ensure_text(options.password)
options.keyspace = maybe_ensure_text(options.keyspace)
```

---

## How to Spot

Any Python 3 CLI tool that reads user-supplied strings via `optparse`/`argparse` and then passes them to APIs that require `str` (not `bytes`) should apply `ensure_text` / `ensure_str` at the boundary where external data enters internal processing. The bug class appears when the normalization is applied inconsistently — some paths get it, others do not — so reviewers should check that all user-visible string options go through the same coercion.

---

## Test

No test added.
