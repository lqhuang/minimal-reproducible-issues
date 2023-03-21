import io.circe.{Codec, Encoder, Json, Decoder}
import io.circe.syntax.*
import io.circe.parser.{decode, parse}
import io.circe.JsonObject

@main def Main() = {
  class Foo(val i: Int, val s: String) {
    override def toString(): String = s"Foo($i, $s)"
  }

  object Foo {
    def apply(i: Int, s: String): Foo = new Foo(i, s)

    given Codec.AsObject[Foo] = {
      Codec.AsObject.from(
        Decoder.instance(c =>
          for {
            i <- c.downField("i").as[Int]
            s <- c.downField("s").as[String]
          } yield Foo(i, s)
        ),
        Encoder.AsObject.instance[Foo](x =>
          JsonObject(
            "i" -> Json.fromInt(x.i),
            "s" -> Json.fromString(x.s)
          )
        )
      )
    }
  }

  parse("""{"i": 1, "s": "string"}""").flatMap(_.as[Foo]) match {
    case Left(error) => println(error)
    case Right(foo)  => println(foo)
  }
}
