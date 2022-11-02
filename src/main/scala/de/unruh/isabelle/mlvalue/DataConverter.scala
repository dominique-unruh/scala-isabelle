package de.unruh.isabelle.mlvalue

import de.unruh.isabelle.control.Isabelle
import de.unruh.isabelle.mlvalue.MLValue.{Converter, Ops}

import scala.concurrent.Future

/**
 * [[MLValue.Converter]] for [[control.Isabelle.Data Isabelle.Data]]s.
 *
 *  - ML type: `data`
 *  - Encoding of an `d : data` as an exception: `E_Data d`
 *
 * @see MLValue.Converter for explanations what [[MLValue.Converter Converter]]s are for.
 */
object DataConverter extends Converter[Isabelle.Data] {
  override def mlType(implicit isabelle: Isabelle): String = "data"

  override def retrieve(value: MLValue[Isabelle.Data])(implicit isabelle: Isabelle): Future[Isabelle.Data] =
    Ops.retrieveData(value)

  override def store(value: Isabelle.Data)(implicit isabelle: Isabelle): MLValue[Isabelle.Data] =
    Ops.storeData(value)

  override def exnToValue(implicit isabelle: Isabelle): String = "fn E_Data data => data"
  override def valueToExn(implicit isabelle: Isabelle): String = "E_Data"
}
