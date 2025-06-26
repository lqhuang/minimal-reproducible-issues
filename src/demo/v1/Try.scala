package redef.util

import scala.runtime.Statics
import scala.util.control.NonFatal

type Try[+T] = Result[T, Throwable]

extension [T](result: Try[T]) {

  def get: T = result match {
    case Ok(value)          => value.asInstanceOf[T]
    case Failure(exception) => throw (exception.asInstanceOf[Throwable])
  }

  inline def failed: Try[Throwable] =
    result.swap.asInstanceOf[Try[Throwable]]

  def collect[U](pf: PartialFunction[T, U]): Try[U] = result match {
    case Failure(exception) => this.asInstanceOf[Try[U]]
    case Ok(value)          =>
      // val marker = Statics.pfMarker
      try
        if pf.isDefinedAt(value)
        then Ok(pf(value))
        else
          Failure(
            new NoSuchElementException(
              "Partial function not defined for " + value
            )
          )
      catch case NonFatal(e) => Failure(e)
  }

  def filter(p: T => Boolean): Try[T] = result match {
    case Failure(exception) => result
    case Ok(value) =>
      try
        if p(value)
        then result
        else
          Failure(
            new NoSuchElementException(s"Predicate does not hold for ${value}")
          )
      catch case NonFatal(exc) => Failure(exc)
  }

  inline final def withFilter(pred: T => Boolean): WithFilter[T] =
    WithFilter(result, pred)

  def recoverWith[U >: T](
      pf: PartialFunction[Throwable, Try[U]]
  ): Try[U] =
    result match {
      case Ok(_) => result
      case Failure(exception) =>
        val marker = Statics.pfMarker
        try
          val v = pf.applyOrElse(exception, (x: Throwable) => marker)
          if marker ne v.asInstanceOf[AnyRef]
          then v.asInstanceOf[Try[U]]
          else result
        catch case NonFatal(e) => Failure(e)
    }

  def recover[U >: T](pf: PartialFunction[Throwable, U]): Try[U] =
    result match {
      case Ok(_) => result
      case Failure(exc) =>
        val marker = Statics.pfMarker
        try
          if pf.isDefinedAt(exc)
          then Ok(pf(exc))
          else result
        catch case NonFatal(e) => Failure(e)
    }

}

object Try {

  def apply[T](r: => T): Try[T] =
    try
      val r1 = r
      Ok(r1)
    catch case NonFatal(e) => Failure(e)

}

private final class WithFilter[T](res: Try[T], p: T => Boolean) {
  def map[U](f: T => U): Try[U] = res.filter(p).map(f)

  def flatMap[U](f: T => Try[U]): Try[U] = res.filter(p).flatMap(f)

  def foreach[U](f: T => U): Unit = res.filter(p).foreach(f)
}

opaque type SafeTry[+T, +E <: Exception] = Result[T, E]
object SafeTry {
  def apply[T, E <: Exception](r: => T throws E): SafeTry[T, E] =
    try
      val r1 = r
      Ok(r1)
    catch case e: E => Failure(e)

  def ok[T, E <: Exception](value: T): SafeTry[T, E] = Ok(value)

  def failure[T, E <: Exception](err: E): SafeTry[T, E] = Failure(err)
}
