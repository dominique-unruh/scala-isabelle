package de.unruh.isabelle.mlvalue

import de.unruh.isabelle.control.Isabelle
import de.unruh.isabelle.mlvalue.MLValue.{Converter, Ops}

import scala.concurrent.Future

// Implicits
import de.unruh.isabelle.control.Isabelle.executionContext

/**
 * [[MLValue.Converter]] for [[scala.Unit Unit]]s.
 *
 *  - ML type: `unit`
 *  - Encoding of the unit value as an exception: `E_Int 0`
 *
 * @see MLValue.Converter for explanations what [[MLValue.Converter Converter]]s are for.
 */
object UnitConverter extends Converter[Unit] {
  override def retrieve(value: MLValue[Unit])(implicit isabelle: Isabelle): Future[Unit] =
    for (_ <- value.id) yield ()

  override def store(value: Unit)(implicit isabelle: Isabelle): MLValue[Unit] = {
    Ops.unitValue
  }

  override def exnToValue(implicit isabelle: Isabelle): String = "K()"
  override def valueToExn(implicit isabelle: Isabelle): String = "K(E_Int 0)"

  override def mlType(implicit isabelle: Isabelle): String = "unit"
}
