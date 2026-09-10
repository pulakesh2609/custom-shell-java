# Custom Shell (Java)

A POSIX-like shell implemented from scratch in Java — supports builtin commands, external program execution, I/O redirection, pipelines, and shell-style quoting.

## Features

- **Builtins**: `echo`, `type`, `pwd`, `cd`, `exit`
- **External program execution** — resolves and runs any executable found in `PATH`, with full argument passing
- **I/O redirection** — `>` / `1>` (stdout), `2>` (stderr), plus append variants `>>`, `1>>`, `2>>`
- **Pipelines** — chain any number of commands with `|`, mixing builtins and external programs freely
- **Quoting** — single quotes, double quotes, and backslash escaping, following real shell parsing rules
- **Directory navigation** — `cd` with absolute paths, relative paths, and home directory (`~`)

## Example usage