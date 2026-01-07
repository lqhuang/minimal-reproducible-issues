//> using scala 2.13
//> using platform scala-native
//> using nativeVersion 0.5.9

import java.util.{List => JList}

object MainSN3 {
  def main(args: Array[String]): Unit = {
    val list: JList[Int] = JList.of(1, 2, 3, 4, 5)
    val afterFilter = list.stream().filter(i => i < 3).toList()
    println(s"Filtered elements: ${afterFilter}")
  }
}
