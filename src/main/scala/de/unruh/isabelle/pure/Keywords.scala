package de.unruh.isabelle.pure

import de.unruh.isabelle.mlvalue.MLValue

/** Represents a specification of Isar keywords as given in a theory header (ML type `Thy_Header.keywords`)
 * in the Isabelle process.
 *
 * An instance of this class is merely a thin wrapper around an [[mlvalue.MLValue MLValue]],
 * all explanations and examples given for [[Context]] also apply here.
 *
 * An implict [[MLValue.Converter]] can be imported from [[Implicits]]`._`. The representation
 * of keywords `keywords` as an ML exception is `E_Keywords keywords`.
 */
final class Keywords private (val mlValue: MLValue[Keywords]) extends MLValueWrapper[Keywords]

object Keywords extends MLValueWrapper.Companion[Keywords] {
  override protected val mlType: String = "Thy_Header.keywords"
  override protected val predefinedException: String = "E_Keywords"
  override protected def instantiate(mlValue: MLValue[Keywords]): Keywords = new Keywords(mlValue)
}
