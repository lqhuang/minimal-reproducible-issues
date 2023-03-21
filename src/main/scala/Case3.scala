import scala.quoted.{Quotes, staging, Type}

@main def Case3: Unit = {
  val settings =
    staging.Compiler.Settings.make(compilerArgs = List("-Yexplicit-nulls"))
  given explicitNullsCompiler: staging.Compiler =
    staging.Compiler.make(getClass.getClassLoader)(settings)

  def code(using Quotes) = '{
    import io.circe.{Codec, Encoder, Json, Decoder}
    import io.circe.syntax.*
    import io.circe.parser.{decode, parse}
    import io.circe.JsonObject

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

    parse("{'i': 1, 's': 'string'}").flatMap(_.as[Foo]) match {
      case Left(error) => println(error)
      case Right(foo)  => println(foo)
    }
  }

  staging.run(code)
}
