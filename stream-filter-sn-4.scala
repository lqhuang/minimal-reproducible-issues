//> using scala 3.7.4
//> using platform scala-native
//> using nativeVersion 0.5.9
//> using nativeMode release-full

import java.util.{List as JList}

object MainSN4 {
  def main(args: Array[String]): Unit = {
    val list: JList[Int] = JList.of(1, 2, 3, 4, 5)
    val afterFilter = list.stream().filter(i => i < 3).toList()
    println(s"Filtered elements: ${afterFilter}")
  }
}
