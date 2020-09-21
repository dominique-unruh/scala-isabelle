package de.unruh.isabelle.mlvalue

import de.unruh.isabelle.control.Isabelle
import de.unruh.isabelle.mlvalue.MLValue.{Converter, Ops}

import scala.concurrent.{ExecutionContext, Future}

/**
 * [[MLValue.Converter]] for [[scala.Unit Unit]]s.
 *
 *  - ML type: `unit`
 *  - Encoding of the unit value as an exception: `E_Int 0`
 *
 * @see MLValue.Converter for explanations what [[MLValue.Converter Converter]]s are for.
 */
object UnitConverter extends Converter[Unit] {
  override def retrieve(value: MLValue[Unit])(implicit isabelle: Isabelle, ec: ExecutionContext): Future[Unit] =
    for (_ <- value.id) yield ()

  override def store(value: Unit)(implicit isabelle: Isabelle, ec: ExecutionContext): MLValue[Unit] = {
    Ops.unitValue
  }

  override val exnToValue: String = "K()"
  override val valueToExn: String = "K(E_Int 0)"

  override def mlType: String = "unit"
}
