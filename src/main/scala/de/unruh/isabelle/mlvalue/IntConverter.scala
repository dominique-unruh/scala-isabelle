package de.unruh.isabelle.mlvalue

import de.unruh.isabelle.control.Isabelle
import de.unruh.isabelle.control.Isabelle.DInt
import de.unruh.isabelle.mlvalue.MLValue.{Converter, Ops, matchFailExn}

import scala.concurrent.Future

// Implicits
import de.unruh.isabelle.control.Isabelle.executionContext
import Implicits._

/**
 * [[MLValue.Converter]] for [[scala.Int Int]]s.
 *
 *  - ML type: `int`
 *  - Encoding of an integer `i` as an exception: `E_Int i`
 *
 * Note that there is an incompatibility between ML `int` and Scala [[scala.Int Int]].
 * The former is unbounded while the latter is a 32-bit integer. ML `int`s that
 * do not fit are truncated upon retrieval (that is, no overflow exception is thrown!)
 * Use [[BigIntConverter]] if arbitrary length integers should be handled.
 *
 * Note that [[IntConverter]], [[LongConverter]], [[BigIntConverter]] are different [[MLValue.Converter Converter]]s for the same ML type `int`.
 * They have compatible representations as exceptions, they can safely be typecast into each other.
 *
 * @see MLValue.Converter for explanations what [[MLValue.Converter Converter]]s are for.
 */
object IntConverter extends Converter[Int] {
  @inline override def store(value: Int)(implicit isabelle: Isabelle): MLValue[Int] =
    Ops.storeInt(DInt(value))

  @inline override def retrieve(value: MLValue[Int])
                               (implicit isabelle: Isabelle): Future[Int] =
    for (DInt(i) <- Ops.retrieveInt(value)) yield i.toInt

  @inline override def exnToValue(implicit isabelle: Isabelle): String = s"fn E_Int i => i | ${matchFailExn("IntConverter.exnToValue")}"
  @inline override def valueToExn(implicit isabelle: Isabelle): String = "E_Int"

  override def mlType(implicit isabelle: Isabelle): String = "int"
}
