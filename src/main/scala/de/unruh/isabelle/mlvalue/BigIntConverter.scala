package de.unruh.isabelle.mlvalue
import java.math.BigInteger

import de.unruh.isabelle.control.Isabelle
import de.unruh.isabelle.control.Isabelle.{DInt, DList, DString}
import de.unruh.isabelle.mlvalue.MLValue.Ops

import scala.concurrent.Future

// Implicits
import Isabelle.executionContext

/**
 * [[MLValue.Converter]] for [[scala.BigInt BigInt]]s.
 *
 *  - ML type: `int`
 *  - Encoding of an integer `i` as an exception: `E_Int i`
 *
 * Note that [[IntConverter]], [[LongConverter]], [[BigIntConverter]] are different [[MLValue.Converter Converter]]s for the same ML type `int`.
 * They have compatible representations as exceptions, they can safely be typecast into each other.
 *
 * @see MLValue.Converter for explanations what [[MLValue.Converter Converter]]s are for.
 */

object BigIntConverter extends MLValue.Converter[BigInt] {
  override def mlType(implicit isabelle: Isabelle): String = IntConverter.mlType

  override def retrieve(value: MLValue[BigInt])(implicit isabelle: Isabelle): Future[BigInt] =
    for (DString(str) <- Ops.retrieveBigInt(value))
      yield BigInt(str.replace('~','-'))

  override def store(value: BigInt)(implicit isabelle: Isabelle): MLValue[BigInt] =
    Ops.storeBigInt(DString(value.toString))

  override def exnToValue(implicit isabelle: Isabelle): String = IntConverter.exnToValue

  override def valueToExn(implicit isabelle: Isabelle): String = IntConverter.valueToExn
}
