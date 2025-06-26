package demo.v1

import scala.annotation.{implicitNotFound, experimental}
import scala.collection.immutable.Seq
import scala.annotation.targetName

sealed abstract trait Result[+T, +E] extends Product with Serializable {
  def isOk: Boolean

  def isFailure: Boolean

  def getOrElse[U >: T](default: => U): U = this match {
    case Ok(t)      => t
    case Failure(e) => default // if default throws, then this will throw
  }

  def fold[O](fok: T => O, ffail: E => O): O = this match {
    case Ok(t)      => fok(t)
    case Failure(e) => ffail(e)
  }

  def orElse[U >: T, F >: E](default: => Result[U, F]): Result[U, F] =
    this match {
      case Ok(_)      => this
      case Failure(_) => default
    }

  def foreach[U](f: T => U): Unit = this match {
    case Ok(t)      => f(t)
    case Failure(_) => ()
  }

  def flatMap[U, F >: E](f: T => Result[U, F]): Result[U, F] = this match {
    case Ok(t)      => f(t)
    case Failure(e) => Failure(e)
  }

  def map[U](f: T => U): Result[U, E] = this match {
    case Ok(t)      => Ok(f(t))
    case Failure(e) => Failure(e)
  }

  def filterOrElse[F >: E](p: T => Boolean, default: => F): Result[T, F] =
    this match {
      case Ok(t) if !p(t) => Failure(default)
      case _              => this
    }

  def flatten[U, F >: E](using
      @implicitNotFound("${T} is not a Result[${U}, ${F}]")
      ev: T <:< Result[U, F]
  ): Result[U, F] = flatMap(ev)

  def flatten[U, F >: E](defaultFailure: => F)(using
      @implicitNotFound("${T} is not a Option[${U}]")
      ev: T <:< Option[U]
  ): Result[U, F] = this match {
    case Ok(ok) =>
      ev(ok) match
        case Some(u) => Ok(u)
        case None    => Failure(defaultFailure)
    case Failure(e) => Failure(e)
  }

  def transform[U >: T, F >: E](
      fok: T => Result[U, F],
      ffail: E => Result[U, F]
  ): Result[U, F] = this match {
    case Ok(t)      => fok(t)
    case Failure(e) => ffail(e)
  }

  def swap: Result[E, T] = this match {
    case Ok(t)      => Failure(t)
    case Failure(e) => Ok(e)
  }

  def exists(p: T => Boolean): Boolean = this match {
    case Ok(t) => p(t)
    case _     => false
  }

  def forall(p: T => Boolean): Boolean = this match {
    case Ok(t) => p(t)
    case _     => true
  }

  def contains[U >: T](x: => U): Boolean = this match {
    case Ok(t) => t == x
    case _     => false
  }

  @experimental
  def to[V](using fromResult: FromResult[T, E, V]): V = fromResult(this)

  def toOption: Option[T] =
    to[Option[T]](using FromResult.optionFromResult[T, E])

  def toEither: Either[E, T] =
    to[Either[E, T]](using FromResult.eitherFromResult[T, E])

  def toSeq: Seq[T] = to[Seq[T]](using FromResult.seqFromResult[T, E])

  // def toTry(using ev: A <:< Throwable): Try[T] = this match {
  //   case Failure(e) => ev(e)
  //   case _          => this
  // }

  // def toSaferTry(using ev: A <:< Throwable): Try[T] = this match {
  //   case Failure(e) => Failure(ev(e))
  //   case _          => this
  // }

  /// new methods

  @experimental
  def or[U >: T, F >: E](default: => Result[U, F]): Result[U, F] = this match {
    case Failure(_) => default
    case _          => this
  }

  def recoverWith[U >: T, F](rf: E => Result[U, F]): Result[U, F] = this match {
    case Ok(t)      => Ok(t)
    case Failure(e) => rf(e)
  }

  def recover[F](rf: E => F): Result[T, F] = this match {
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
  ): Result[U, F] = this match {
    case Ok(t) => this.asInstanceOf[Result[U, F]]
    case Failure(e) =>
      ev(e) match {
        case Ok(u)      => Ok(u)
        case Failure(f) => Failure(f)
      }
  }

  @experimental
  def ok: Option[T] = this match {
    case Ok(t)      => Some(t)
    case Failure(_) => None
  }

  @experimental
  def failure: Option[E] = this match {
    case Ok(_)      => None
    case Failure(e) => Some(e)
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

  implicit class MergeOps[A](private val r: Result[A, A]) extends AnyVal {
    def merge: A = r match {
      case Ok(v)      => v
      case Failure(v) => v
    }
  }

  def ok[T, E](value: T): Result[T, E] = Ok(value)

  def failure[T, E](err: E): Result[T, E] = Failure(err)
}

case class Ok[+T, +E](value: T) extends Result[T, E] {
  def isOk: Boolean = true

  def isFailure: Boolean = false

  def intoOk: T = value

  def withFailure[F >: E]: Result[T, F] = this
}
object Ok {
  val unit: Result[Unit, Nothing] = Ok(())
}

case class Failure[+T, +E](err: E) extends Result[T, E] {
  def isOk: Boolean = false

  def isFailure: Boolean = true

  def intoFailure: E = err

  def withOk[U >: T]: Result[U, E] = this
}
object Failure {
  val unit: Result[Nothing, Unit] = Failure(())
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
