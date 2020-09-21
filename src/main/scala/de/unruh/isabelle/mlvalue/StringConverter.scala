package de.unruh.isabelle.mlvalue

import de.unruh.isabelle.control.Isabelle
import de.unruh.isabelle.control.Isabelle.DString
import de.unruh.isabelle.mlvalue.MLValue.{Converter, Ops, matchFailExn}

import scala.concurrent.{ExecutionContext, Future}

import Implicits._

/**
 * [[MLValue.Converter]] for [[java.lang.String String]]s.
 *
 *  - ML type: `string`
 *  - Encoding of a string `s` as an exception: `E_String s`
 *
 * Note that there is an incompatibility between ML `string` and Scala [[java.lang.String String]].
 * The former is restricted to characters with codepoints 0...255 (with no specified character set interpretation
 * for characters over 128)
 * and can be at most 67.108.856 characters long (`String.maxSize` in ML). The latter has 16-bit Unicode characters
 * and no length limit. This converter will work correctly for ASCII strings of the maximum ML-length,
 * but throw exceptions when storing longer strings, and replace non-ASCII characters in unspecified ways.
 *
 * @see MLValue.Converter for explanations what [[MLValue.Converter Converter]]s are for.
 */
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
