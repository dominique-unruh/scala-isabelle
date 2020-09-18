package de.unruh.isabelle.mlvalue

import de.unruh.isabelle.control.Isabelle
import de.unruh.isabelle.mlvalue.MLValue.Converter
import scalaz.Id.Id

import scala.concurrent.{ExecutionContext, Future}

// TODO: Document API
@inline final class MLValueConverter[A] extends Converter[MLValue[A]] {
  override def retrieve(value: MLValue[MLValue[A]])(implicit isabelle: Isabelle, ec: ExecutionContext): Future[MLValue[A]] =
    Future.successful(value.removeMLValue[Id, A])

  override def store(value: MLValue[A])(implicit isabelle: Isabelle, ec: ExecutionContext): MLValue[MLValue[A]] =
    value.insertMLValue[Id, A]

  @inline override def exnToValue: String = "fn x => x"
  @inline override def valueToExn: String = "fn x => x"

  override def mlType: String = "_"
}
