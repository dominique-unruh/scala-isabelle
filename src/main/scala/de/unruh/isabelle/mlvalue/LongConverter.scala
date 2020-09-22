package de.unruh.isabelle.mlvalue

import de.unruh.isabelle.control.Isabelle
import de.unruh.isabelle.control.Isabelle.DInt
import de.unruh.isabelle.mlvalue.MLValue.{Converter, Ops, matchFailExn}

import scala.concurrent.{ExecutionContext, Future}

import Implicits._

/**
 * [[MLValue.Converter]] for [[scala.Long Long]]s.
 *
 *  - ML type: `int`
 *  - Encoding of an integer `i` as an exception: `E_Int i`
 *
 * Note that there is an incompatibility between ML `int` and Scala [[scala.Long Long]].
 * The former is unbounded while the latter is a 64-bit integer. ML `int`s that
 * do not fit are truncated upon retrieval (that is, no overflow exception is thrown!)
 * Use [[BigIntConverter]] if arbitrary length integers should be handled.
 *
 * Note that [[IntConverter]], [[LongConverter]], [[BigIntConverter]] are different [[MLValue.Converter Converter]]s for the same ML type `int`.
 * They have compatible representations as exceptions, they can safely be typecast into each other.
 *
 * @see MLValue.Converter for explanations what [[MLValue.Converter Converter]]s are for.
 */
object LongConverter extends Converter[Long] {
  @inline override def store(value: Long)(implicit isabelle: Isabelle, ec: ExecutionContext): MLValue[Long] =
    Ops.storeLong(DInt(value))

  @inline override def retrieve(value: MLValue[Long])
                               (implicit isabelle: Isabelle, ec: ExecutionContext): Future[Long] =
    for (DInt(i) <- Ops.retrieveLong(value.id)) yield i

  @inline override def exnToValue: String = IntConverter.exnToValue
  @inline override def valueToExn: String = IntConverter.valueToExn

  override def mlType: String = IntConverter.mlType
}
