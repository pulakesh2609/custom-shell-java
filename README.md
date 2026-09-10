# Custom Shell (Java)

A command-line shell built from scratch in Java — no shell libraries, no frameworks, just plain Java reading input and deciding what to do with it. 
It behaves like a simplified version of `bash`: you type a command, it runs, you see the result, and it asks for the next one.

This was built as a learning project to understand what's actually happening under the hood every time you type a command into a terminal — how arguments get parsed, how programs get found and launched, and how features like `|` and `>` actually work at the OS level.

## What it can do

**Run any program on your system.** Type `ls`, `grep something file.txt`, or any other program name, and the shell searches your `PATH` environment variable (the same list of folders a real shell checks), finds the matching executable, and runs it — passing along whatever arguments you typed.

**Built-in commands**, handled directly by the shell itself rather than launched as separate programs:

| Command | What it does |
|---|---|
| `echo <text>` | Prints text back to the screen |
| `pwd` | Prints the current working directory |
| `cd <path>` | Changes the current directory — supports absolute paths (`cd /home/user`), relative paths (`cd ..`, `cd src`), and home directory (`cd` alone, or `cd ~`) |
| `type <command>` | Tells you whether something is a builtin or an external program, and if external, shows its full file path |
| `exit` | Closes the shell |

**Redirect output to a file** instead of the screen, using `>` (or the more explicit `1>`):

```
$ echo "hello" > greeting.txt
```

Nothing prints to your terminal — `hello` goes straight into `greeting.txt` instead. Use `>>` if you want to *add* to a file's existing content rather than overwrite it:

```
$ echo "first line" > log.txt
$ echo "second line" >> log.txt
```

`log.txt` now has both lines. The same idea works for error messages specifically, using `2>` and `2>>` — useful when you want normal output and error output to go to two different places.

**Chain commands together with pipes (`|`)** — the output of one command becomes the input of the next:

```
$ ls | grep ".java"
```

This lists files, then filters that list down to only the ones containing ".java" — exactly like a real shell. You can chain as many commands as you like, and it works even when a builtin (like `echo` or `type`) is one of the links in the chain:

```
$ type echo | cat
echo is a shell builtin
```

**Quoting, so spaces and special characters behave correctly:**

```
$ echo 'this stays exactly as typed, no $variables or \backslashes interpreted'
$ echo "this allows some escapes like \" and \\"
$ echo path\ with\ spaces
```

Single quotes preserve everything literally. Double quotes allow a few specific escape sequences. A backslash outside any quotes escapes just the one character right after it (handy for things like spaces in a filename).

## How it works, under the hood

The shell runs a simple loop: **print a prompt → read a line → figure out what it means → do it → repeat.** The interesting part is the "figure out what it means" step, which breaks down into a few stages:

1. **Tokenizing** — the raw text you type (e.g. `echo "hello world" > out.txt`) gets split into meaningful pieces (`echo`, `hello world`, `>`, `out.txt`), correctly handling quotes and escapes so that quoted spaces don't accidentally split a phrase into multiple pieces.
2. **Redirection detection** — the token list is scanned for `>`, `>>`, `2>`, etc. Whichever filename follows one of these gets pulled out, and the actual command's tokens are separated from the redirection instructions.
3. **Pipe splitting** — if a `|` token is present, the remaining tokens are split into separate command "stages," each of which gets executed in sequence, with one stage's output captured and handed to the next stage as input.
4. **Dispatch** — each stage's first word is checked against the list of builtins. If it matches one, the shell handles it directly in Java. If not, the shell searches `PATH` for a matching executable and, if found, launches it as a separate operating-system process using Java's `ProcessBuilder`, connecting its input/output appropriately (to your terminal, to a file, or to the next stage in a pipe).

## Example usage

```
$ pwd
/home/user/projects

$ cd src
$ pwd
/home/user/projects/src

$ echo "Build started" > build.log
$ echo "Compiling..." >> build.log
$ cat build.log
Build started
Compiling...

$ ls | grep .java | wc -l
4

$ type cd
cd is a shell builtin

$ type javac
javac is /usr/bin/javac

$ nonexistent_command
nonexistent_command: command not found
```

## Running it locally

Requires JDK 21 or newer, and Maven.

```bash
mvn compile
java -cp target/classes Main
```

You'll land at a `$ ` prompt — try any of the examples above.

## Built with

- **Java** — the whole shell logic, no external libraries
- **`ProcessBuilder`** — for finding, launching, and wiring up external programs (including chaining several together for pipelines)
- **A hand-written tokenizer** — parses raw input character by character, handling quotes, backslash escapes, and redirection/pipe symbols without relying on a simple `.split(" ")`, which can't correctly handle quoted text