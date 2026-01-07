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
