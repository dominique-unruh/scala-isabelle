package de.unruh.isabelle.mlvalue

import de.unruh.isabelle.control.Isabelle
import de.unruh.isabelle.control.Isabelle.DInt
import de.unruh.isabelle.mlvalue.MLValue.{Converter, Ops, matchFailExn}

import scala.concurrent.{ExecutionContext, Future}

import MLValue.Implicits._

object LongConverter extends Converter[Long] {
  @inline override def store(value: Long)(implicit isabelle: Isabelle, ec: ExecutionContext): MLValue[Long] =
    Ops.storeLong(DInt(value))

  @inline override def retrieve(value: MLValue[Long])
                               (implicit isabelle: Isabelle, ec: ExecutionContext): Future[Long] =
    for (DInt(i) <- Ops.retrieveLong(value.id)) yield i

  override lazy val exnToValue: String = s"fn E_Int i => i | ${matchFailExn("LongConverter.exnToValue")}"
  override lazy val valueToExn: String = "E_Int"
}
