# Scala Native HTTP version shim

Three independent projects using Scala 3.3.7 and Scala Native 0.5.12.
Each defines `net.http.HttpClient.Version.HTTP_3` as a Java-compatible shims in Scala
and prints `HTTP_3` from a native executable.

Requires a JDK, Clang/LLVM, and the corresponding build tool.
The builds pin sbt 1.12.9 and Mill 0.12.9/1.0.6/1.1.9.

| Directory   | Run from that directory  |
| ----------- | ------------------------ |
| `sbt`       | `sbt run`                |
| `mill`      | `./mill --no-server run` |
| `scala-cli` | `scala-cli run .`        |

The Scala distribution's `scala` command also runs the Scala CLI project:
`scala run .`.
