package de.unruh.isabelle.mlvalue

import de.unruh.isabelle.control.Isabelle
import de.unruh.isabelle.control.Isabelle.DInt
import de.unruh.isabelle.mlvalue.MLValue.{Converter, Ops, matchFailExn}

import scala.concurrent.{ExecutionContext, Future}

import MLValue.Implicits._

// TODO: Document API
object LongConverter extends Converter[Long] {
  @inline override def store(value: Long)(implicit isabelle: Isabelle, ec: ExecutionContext): MLValue[Long] =
    Ops.storeLong(DInt(value))

  @inline override def retrieve(value: MLValue[Long])
                               (implicit isabelle: Isabelle, ec: ExecutionContext): Future[Long] =
    for (DInt(i) <- Ops.retrieveLong(value.id)) yield i

  @inline override def exnToValue: String = s"fn E_Int i => i | ${matchFailExn("LongConverter.exnToValue")}"
  @inline override def valueToExn: String = "E_Int"

  override def mlType: String = "int"
}
