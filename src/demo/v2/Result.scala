package demo.v2

import scala.annotation.{implicitNotFound, experimental}
import scala.collection.immutable.Seq
import scala.annotation.targetName

case class Ok[+T, +E](value: T) extends AnyVal with Serializable {
  def intoOk: T = value

  def withFailure[F >: E]: Result[T, F] = this
}
object Ok {
  val unit: Result[Unit, Nothing] = Ok(())
}

case class Failure[+T, +E](err: E) extends AnyVal with Serializable {
  def intoFailure: E = err

  def withOk[U >: T]: Result[U, E] = this
}
object Failure {
  val unit: Result[Nothing, Unit] = Failure(())
}

type Result[+T, +E] = Ok[T] | Failure[E]

extension [T, E](result: Result[T, E]) {
  inline def isOk: Boolean = result match {
    case Ok(_)      => true
    case Failure(_) => false
  }

  inline def isFailure: Boolean = !isOk

  def getOrElse[U >: T](default: => U): U = result match {
    case Ok(t)      => t
    case Failure(e) => default // if default throws, then this will throw
  }

  def fold[O](fok: T => O, ffail: E => O): O = result match {
    case Ok(t)      => fok(t)
    case Failure(e) => ffail(e)
  }

  def orElse[U >: T, F >: E](default: => Result[U, F]): Result[U, F] =
    result match {
      case Ok(_)      => result
      case Failure(_) => default
    }

  def foreach[U](f: T => U): Unit = result match {
    case Ok(t)      => f(t)
    case Failure(_) => ()
  }

  def flatMap[U, F >: E](f: T => Result[U, F]): Result[U, F] = result match {
    case Ok(t)      => f(t)
    case Failure(e) => Failure(e)
  }

  def map[U](f: T => U): Result[U, E] = result match {
    case Ok(t)      => Ok(f(t))
    case Failure(e) => Failure(e)
  }

  def filterOrElse[F >: E](p: T => Boolean, default: => F): Result[T, F] =
    result match {
      case Ok(t) if !p(t) => Failure(default)
      case _              => result
    }

  def flatten[U, F >: E](using
      @implicitNotFound("${T} is not a Result[${U}, ${F}]")
      ev: T <:< Result[U, F]
  ): Result[U, F] = flatMap(ev)

  def flatten[U, F >: E](defaultFailure: => F)(using
      @implicitNotFound("${T} is not a Option[${U}]")
      ev: T <:< Option[U]
  ): Result[U, F] = result match {
    case Ok(ok) =>
      ev(ok) match
        case Some(u) => Ok(u)
        case None    => Failure(defaultFailure)
    case Failure(e) => Failure(e)
  }

  def transform[U >: T, F >: E](
      fok: T => Result[U, F],
      ffail: E => Result[U, F]
  ): Result[U, F] = result match {
    case Ok(t)      => fok(t)
    case Failure(e) => ffail(e)
  }

  def swap: Result[E, T] = result match {
    case Ok(t)      => Failure(t)
    case Failure(e) => Ok(e)
  }

  def exists(p: T => Boolean): Boolean = result match {
    case Ok(t) => p(t)
    case _     => false
  }

  def forall(p: T => Boolean): Boolean = result match {
    case Ok(t) => p(t)
    case _     => true
  }

  def contains[U >: T](x: => U): Boolean = result match {
    case Ok(t) => t == x
    case _     => false
  }

  @experimental
  def to[V](using fromResult: FromResult[T, E, V]): V = fromResult(result)

  def toOption: Option[T] =
    to[Option[T]](using FromResult.optionFromResult[T, E])

  def toEither: Either[E, T] =
    to[Either[E, T]](using FromResult.eitherFromResult[T, E])

  def toSeq: Seq[T] = to[Seq[T]](using FromResult.seqFromResult[T, E])

  // def toTry(using ev: A <:< Throwable): Try[T] = result match {
  //   case Failure(e) => ev(e)
  //   case _          => result
  // }

  // def toSaferTry(using ev: A <:< Throwable): Try[T] = result match {
  //   case Failure(e) => Failure(ev(e))
  //   case _          => result
  // }

  /// new methods

  @experimental
  def or[U >: T, F >: E](default: => Result[U, F]): Result[U, F] =
    result match {
      case Failure(_) => default
      case _          => result
    }

  def recoverWith[U >: T, F](rf: E => Result[U, F]): Result[U, F] =
    result match {
      case Ok(t)      => Ok(t)
      case Failure(e) => rf(e)
    }

  def recover[F](rf: E => F): Result[T, F] = result match {
    case Ok(t)      => Ok(t)
    case Failure(e) => Failure(rf(e))
  }

  def joinOk[U, F >: E](using
      @implicitNotFound("${T} is not a Result[${U}, ${F}]")
      ev: T <:< Result[U, F]
  ): Result[U, F] = flatten(using ev)

  def joinFailure[U >: T, F](using
      @implicitNotFound("${E} is not a Result[${U}, ${F}]")
      ev: E <:< Result[U, F]
  ): Result[U, F] = result match {
    case Ok(t) => result.asInstanceOf[Result[U, F]]
    case Failure(e) =>
      ev(e) match {
        case Ok(u)      => Ok(u)
        case Failure(f) => Failure(f)
      }
  }

  @experimental
  def ok: Option[T] = result match {
    case Ok(t)      => Some(t)
    case Failure(_) => None
  }

  @experimental
  def failure: Option[E] = result match {
    case Ok(_)      => None
    case Failure(e) => Some(e)
  }
}

extension [A](result: Result[A, A]) {
  def merge: A = result match {
    case Ok(v)      => v
    case Failure(v) => v
  }
}

object Result {
  def cond[T, E](
      test: Boolean,
      ok: => T,
      failure: => E
  ): Result[T, E] = if (test) Ok(ok) else Failure(failure)

  def from[T, E, V](v: V)(using toResult: ToResult[T, E, V]): Result[T, E] =
    toResult(v)

  def ok[T, E](value: T): Result[T, E] = Ok(value)

  def failure[T, E](err: E): Result[T, E] = Failure(err)
}

trait FromResult[-T, -E, +V] {
  def apply(result: Result[T, E]): V
}

object FromResult {
  implicit def optionFromResult[T, E]: FromResult[T, E, Option[T]] = {
    case Ok(t)      => Some(t)
    case Failure(_) => None
  }

  implicit def eitherFromResult[T, E]: FromResult[T, E, Either[E, T]] = {
    case Ok(t)      => Right(t)
    case Failure(e) => Left(e)
  }

  implicit def seqFromResult[T, E]: FromResult[T, E, Seq[T]] = {
    case Ok(t)      => Seq(t)
    case Failure(_) => Seq.empty
  }

  // implicit def tryFromResult[T]: FromResult[T, Exception, Try[T]] = {
  //   case Ok(t)      => Success(t)
  //   case Failure(e) => Failure(e)
  // }

  implicit def scuTryFromResult[T]
      : FromResult[T, Throwable, scala.util.Try[T]] = {
    case Ok(t)      => scala.util.Success(t)
    case Failure(e) => scala.util.Failure(e)
  }

}

trait ToResult[+T, +E, -V] {
  def apply(value: V): Result[T, E]
}
object ToResult {

  implicit def eitherToResult[T, E]: ToResult[T, E, Either[E, T]] = {
    case Right(ok) => Ok(ok)
    case Left(e)   => Failure(e)
  }

  implicit def scuTryToResult[T]: ToResult[T, Throwable, scala.util.Try[T]] = {
    case scala.util.Success(v) => Ok(v)
    case scala.util.Failure(e) => Failure(e)
  }

  implicit val booleanToResult: ToResult[Unit, Unit, Boolean] = {
    case true  => Ok.unit
    case false => Failure.unit
  }
}
