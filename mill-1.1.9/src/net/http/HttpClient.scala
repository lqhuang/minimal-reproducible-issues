package java.net.http

import java.lang._Enum

abstract class HttpClient extends AutoCloseable

object HttpClient:

  sealed class Version private (name: String, ordinal: Int)
      extends _Enum[Version](name, ordinal)

  object Version:

    final val HTTP_1_1 = new Version("HTTP_1_1", 0)
    final val HTTP_2 = new Version("HTTP_2", 1)
    final val HTTP_3 = new Version("HTTP_3", 2)

    def values(): Array[Version] = Array(HTTP_1_1, HTTP_2, HTTP_3)

    def valueOf(name: String): Version =
      name match
        case "HTTP_1_1" => HTTP_1_1
        case "HTTP_2"   => HTTP_2
        case "HTTP_3"   => HTTP_3
        case _          =>
          throw new IllegalArgumentException(
            s"No enum constant HttpClient.Version.$name"
          )

  end Version

end HttpClient
