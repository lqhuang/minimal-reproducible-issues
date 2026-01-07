# Java Stream filter returning wrong results in Scala Native

This repository contains a minimal reproducible example demonstrating an issue with using Java Streams' `filter` method in Scala Native. The problem manifests when filtering elements from a Java List, where the filtered results are incorrect or unexpected.

## Reproduction Steps

Using `scala-cli` to run the examples:

```scala
//> using scala 3.3.7
//> using platform scala-native
//> using nativeVersion 0.5.8

import java.util.List as JList

object MainSN1 {
  def main(args: Array[String]): Unit = {
    val list: JList[Int] = JList.of(1, 2, 3, 4, 5)
    val afterFilter = list.stream().filter(i => i < 3).toList()
    println(s"Filtered elements: ${afterFilter}")
  }
}
```

and the return output is:

```
Filtered elements: [1, 2, null, null, null]
```

while using Scala JVM

```scala
//> using scala 3.3.7

import java.util.List as JList

object Main {
  def main(args: Array[String]): Unit = {
    val list: JList[Int] = JList.of(1, 2, 3, 4, 5)
    val afterFilter = list.stream().filter(i => i < 3).toList()
    println(s"Filtered elements: ${afterFilter}")
  }
}
```

and the return output is:

```
Filtered elements: [1, 2]
```
