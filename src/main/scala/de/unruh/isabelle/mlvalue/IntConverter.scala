package de.unruh.isabelle.mlvalue

import de.unruh.isabelle.control.Isabelle
import de.unruh.isabelle.control.Isabelle.DInt
import de.unruh.isabelle.mlvalue.MLValue.{Converter, Ops, matchFailExn}

import scala.concurrent.{ExecutionContext, Future}

import Implicits._

// TODO: Document API
object IntConverter extends Converter[Int] {
  @inline override def store(value: Int)(implicit isabelle: Isabelle, ec: ExecutionContext): MLValue[Int] =
    Ops.storeInt(DInt(value))

  @inline override def retrieve(value: MLValue[Int])
                               (implicit isabelle: Isabelle, ec: ExecutionContext): Future[Int] =
    for (DInt(i) <- Ops.retrieveInt(value.id)) yield i.toInt

  @inline override def exnToValue: String = s"fn E_Int i => i | ${matchFailExn("IntConverter.exnToValue")}"
  @inline override def valueToExn: String = "E_Int"

  override def mlType: String = "int"
}
