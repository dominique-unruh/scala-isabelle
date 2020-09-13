package de.unruh.isabelle.mlvalue

import de.unruh.isabelle.control.Isabelle
import de.unruh.isabelle.control.Isabelle.DString
import de.unruh.isabelle.mlvalue.MLValue.{Converter, Ops, matchFailExn}

import scala.concurrent.{ExecutionContext, Future}

import MLValue.Implicits._

object StringConverter extends Converter[String] {
  @inline override def store(value: String)(implicit isabelle: Isabelle, ec: ExecutionContext): MLValue[String] =
    Ops.storeString(DString(value))

  @inline override def retrieve(value: MLValue[String])
                               (implicit isabelle: Isabelle, ec: ExecutionContext): Future[String] =
    for (DString(str) <- Ops.retrieveString(value.id))
      yield str

  override lazy val exnToValue: String = s"fn E_String str => str | ${matchFailExn("BooleanConverter.exnToValue")}"
  override lazy val valueToExn: String = "E_String"
}
