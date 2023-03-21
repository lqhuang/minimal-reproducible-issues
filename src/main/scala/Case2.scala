import java.{math => jm}
import java.time.Instant
import java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME
import java.time.temporal.ChronoUnit.MILLIS

import io.circe.{Codec, Encoder, Json, Decoder}
import io.circe.syntax.*
import io.circe.parser.{decode, parse}

import scala.quoted.{Quotes, staging, Type}
import io.circe.JsonObject

@main def Case2: Unit =
  val rawJson: String = """
    {
      "id": 13,
      "name": "test-event",
      "eventTime": "2023-01-05T08:04:55.697Z"
    }
    """
  // val inEvent = parse(rawJson)
  //   .flatMap(_.as[InEvent])
  //   .getOrElse(InEvent(0, "error", Instant.now().nn))
  // println(s"${inEvent}")
  // println(msg)

  case class Foo(i: jm.BigInteger, j: Int, s: String)
  case class Bar[A](a: A)
  case class Event(id: Int, name: String, eventTime: Instant)
      derives Codec.AsObject

  import scala.quoted.{Quotes, staging, Type}

  val settings =
    staging.Compiler.Settings.make(compilerArgs = List("-Yexplicit-nulls"))
  given explicitNullsCompiler: staging.Compiler =
    staging.Compiler.make(getClass.getClassLoader)(settings)

  def code(using Quotes) = '{

    class Event(val id: Int, val name: String, val eventTime: Instant)

    object Event {
      // given Codec[Event] = Codec.AsObject.derived[Event]
      given Codec.AsObject[Event] = {
        Codec.AsObject.from(
          Decoder.instance(c =>
            for {
              id <- c.downField("id").as[Int]
              name <- c.downField("name").as[String]
              eventTime <- c.downField("eventTime").as[Instant]
            } yield Event(id, name, eventTime)
          ),
          Encoder.AsObject.instance[Event](e =>
            JsonObject(
              "id" -> Json.fromInt(e.id),
              "name" -> Json.fromString(e.name),
              "eventTime" -> Json.fromString(e.eventTime.toString())
            )
          )
        )
      }
    }

    given Encoder[Instant] =
      Encoder.encodeString.contramap[Instant](t =>
        ISO_OFFSET_DATE_TIME.nn.format(t.truncatedTo(MILLIS).nn).nn
      )

    println("Fuck! GPT-4 is crazy ...")

  }
  staging.run(code)

def msg = "I was compiled by Scala 3 with Circe :)"
