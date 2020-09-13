package de.unruh.isabelle.mlvalue

import de.unruh.isabelle.control.Isabelle
import de.unruh.isabelle.control.Isabelle.{DList, DObject}
import de.unruh.isabelle.mlvalue.MLValue.{Converter, Ops, matchFailExn}

import scala.concurrent.{ExecutionContext, Future}

import MLValue.Implicits._

@inline class OptionConverter[A](implicit converter: Converter[A]) extends Converter[Option[A]] {
  @inline override def store(value: Option[A])(implicit isabelle: Isabelle, ec: ExecutionContext): MLValue[Option[A]] = value match {
    case None => Ops.optionNone
    case Some(x) =>
      Ops.optionSome(x)
  }

  @inline override def retrieve(value: MLValue[Option[A]])(implicit isabelle: Isabelle, ec: ExecutionContext): Future[Option[A]] = {
    for (data <- Ops.retrieveOption(value.id);
         option <- data match {
           case DList() => Future.successful(None): Future[Option[A]]
           case DList(DObject(id)) => converter.retrieve(new MLValue[A](Future.successful(id))).map(Some(_)): Future[Option[A]]
         })
      yield option
  }

  override lazy val exnToValue: String = s"fn E_Option x => Option.map (${converter.exnToValue}) x | ${matchFailExn("OptionConverter.exnToValue")}"
  override lazy val valueToExn: String = s"E_Option o Option.map (${converter.valueToExn})"
}
