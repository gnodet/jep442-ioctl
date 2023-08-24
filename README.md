# jep442-ioctl

Reproducer for the IOCTL function call problem with JEP 442 (and previous).

## Build

To reproduce the issue on Linux or MacOS, clone this repository and run the [`build.sh`](build.sh) script included.  This will compile the [java class](Reproducer.java) and run it.

## Explanation

This simple example uses only two methods:
 * `openpty` to create a new virtual terminal
 * `ioctl` to retrieve the terminal size

This has been working with other environments (JNA, or plain JNI), but causes a crash with JEP 442 (and previous versions too).  An [example dump](hs_err_pid73099.log) has been attached.

The output looks like:
```
➜  jep442-ioctl git:(main) ✗ ./build.sh
Note: Reproducer.java uses preview features of Java SE 21.
Note: Recompile with -Xlint:preview for details.
Opening pty...
Sucessfully opened pty (/dev/ttys007, 4, 5)
Getting pty size...
#
# A fatal error has been detected by the Java Runtime Environment:
#
#  SIGSEGV (0xb) at pc=0x000000011678504c, pid=73851, tid=9219
#
# JRE version: OpenJDK Runtime Environment (21.0+35) (build 21+35-2513)
# Java VM: OpenJDK 64-Bit Server VM (21+35-2513, mixed mode, sharing, tiered, compressed oops, compressed class ptrs, g1 gc, bsd-aarch64)
# Problematic frame:
# 
[error occurred during error reporting (printing problematic frame), id 0xb, SIGSEGV (0xb) at pc=0x00000001061ac0fc]
# No core dump will be written. Core dumps have been disabled. To enable core dumping, try "ulimit -c unlimited" before starting Java again
#
# An error report file with more information is saved as:
# /Users/gnodet/work/git/jep442-ioctl/hs_err_pid73851.log
[0.106s][warning][os] Loading hsdis library failed
#
# If you would like to submit a bug report, please visit:
#   https://bugreport.java.com/bugreport/crash.jsp
#
```
