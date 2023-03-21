import io.circe.{Codec, Encoder, Json, Decoder}
import io.circe.syntax.*
import io.circe.parser.{decode, parse}

import scala.quoted.staging
import scala.quoted.{Quotes, Type}

@main def Case1: Unit = {
  val settings =
    staging.Compiler.Settings.make(compilerArgs = List("-Yexplicit-nulls"))
  given explicitNullsCompiler: staging.Compiler =
    staging.Compiler.make(getClass.getClassLoader)(settings)

  def code(using Quotes) = '{
    case class Foo(val i: Int, val s: String)

    given Codec[Foo] = Codec.AsObject.derived[Foo]
    given Decoder[Foo] = Decoder.derived[Foo]
    given Encoder[Foo] = Encoder.AsObject.derived[Foo]

    println(
      "Case class definitions are not allowed in inline methods or quoted code. Use a normal class instead."
    )

  }

  staging.run(code)
}
