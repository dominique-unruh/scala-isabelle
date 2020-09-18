package de.unruh.isabelle.mlvalue

import de.unruh.isabelle.control.Isabelle
import de.unruh.isabelle.control.Isabelle.DString
import de.unruh.isabelle.mlvalue.MLValue.{Converter, Ops, matchFailExn}

import scala.concurrent.{ExecutionContext, Future}

import Implicits._

// TODO: Document API
object StringConverter extends Converter[String] {
  @inline override def store(value: String)(implicit isabelle: Isabelle, ec: ExecutionContext): MLValue[String] =
    Ops.storeString(DString(value))

  @inline override def retrieve(value: MLValue[String])
                               (implicit isabelle: Isabelle, ec: ExecutionContext): Future[String] =
    for (DString(str) <- Ops.retrieveString(value.id))
      yield str

  @inline override def exnToValue: String = s"fn E_String str => str | ${matchFailExn("BooleanConverter.exnToValue")}"
  @inline override def valueToExn: String = "E_String"

  override def mlType: String = "string"
}
