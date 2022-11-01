package de.unruh.isabelle.mlvalue

import de.unruh.isabelle.control.Isabelle
import de.unruh.isabelle.control.Isabelle.DInt
import de.unruh.isabelle.mlvalue.MLValue.{Converter, Ops, matchFailExn}

import scala.concurrent.{ExecutionContext, Future}

// Implicits
import de.unruh.isabelle.control.Isabelle.executionContext

/**
 * [[MLValue.Converter]] for [[scala.Boolean Boolean]]s.
 *
 *  - ML type: `bool`
 *  - Encoding of a bool `b` as an exception: `E_Bool b`
 *
 * @see MLValue.Converter for explanations what [[MLValue.Converter Converter]]s are for.
 */
object BooleanConverter extends Converter[Boolean] {
  override def retrieve(value: MLValue[Boolean])(implicit isabelle: Isabelle): Future[Boolean] = {
    implicitly[ExecutionContext]
    for (DInt(i) <- Ops.retrieveBool(value))
      yield i != 0
  }

  override def store(value: Boolean)(implicit isabelle: Isabelle): MLValue[Boolean] =
    if (value) Ops.boolTrue else Ops.boolFalse

  @inline override def exnToValue(implicit isabelle: Isabelle): String = s"fn E_Bool b => b | ${matchFailExn("BooleanConverter.exnToValue")}"
  @inline override def valueToExn(implicit isabelle: Isabelle): String = "E_Bool"

  override def mlType(implicit isabelle: Isabelle): String = "bool"
}
