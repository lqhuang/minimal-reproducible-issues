package libcurl

import scala.scalanative.unsafe.fromCString

object Main:
  def main(args: Array[String]): Unit =
    println("Hello, libcurl! -> " ++ fromCString(functions.curlVersion()))
