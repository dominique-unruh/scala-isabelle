package de.unruh.isabelle.mlvalue

import de.unruh.isabelle.control.Isabelle
import de.unruh.isabelle.control.Isabelle.{DList, DObject}
import de.unruh.isabelle.mlvalue.MLValue.{Converter, Ops, matchFailExn}

import scala.concurrent.{ExecutionContext, Future}

import Implicits._

/**
 * [[MLValue.Converter]] for type [[Option]][A].
 *
 *  - ML type: `a option` (if `a` is the ML type corresponding to `A`).
 *  - Encoding of `Some(x)` and `None` as an exception:
 *    `E_Option (SOME e)` and `E_Option NONE` `e`
 *    is the encoding of `x` as an exception (according to the converter for `A`).
 *
 * @see MLValue.Converter for explanations what [[MLValue.Converter Converter]]s are for.
 */
@inline final class OptionConverter[A](implicit converter: Converter[A]) extends Converter[Option[A]] {
  @inline override def store(value: Option[A])(implicit isabelle: Isabelle, ec: ExecutionContext): MLValue[Option[A]] = value match {
    case None => Ops.optionNone
    case Some(x) =>
      Ops.optionSome(x)
  }

  @inline override def retrieve(value: MLValue[Option[A]])(implicit isabelle: Isabelle, ec: ExecutionContext): Future[Option[A]] = {
    for (data <- Ops.retrieveOption(value.id);
         option <- data match {
           case DList() => Future.successful(None): Future[Option[A]]
           case DList(DObject(id)) => converter.retrieve(MLValue.unsafeFromId[A](Future.successful(id))).map(Some(_)): Future[Option[A]]
         })
      yield option
  }

  @inline override def exnToValue: String = s"fn E_Option x => Option.map (${converter.exnToValue}) x | ${matchFailExn("OptionConverter.exnToValue")}"
  @inline override def valueToExn: String = s"E_Option o Option.map (${converter.valueToExn})"

  override def mlType: String = s"(${converter.mlType}) option"
}
