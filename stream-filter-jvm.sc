//> using scala 3.3.7

import java.util.List as JList

val list: JList[Int] = JList.of(1, 2, 3, 4, 5)
val afterFilter = list.stream().filter(i => i < 3).toList()
println(s"Filtered elements: ${afterFilter}")
