package de.unruh.isabelle.pure

import de.unruh.isabelle.mlvalue.MLValue

// DOCUMENT
final class Keywords private (val mlValue: MLValue[Keywords]) extends MLValueWrapper[Keywords]

object Keywords extends MLValueWrapper.Companion[Keywords] {
  override protected val mlType: String = "Thy_Header.keywords"
  override protected val predefinedException: String = "E_Keywords"
  override protected def instantiate(mlValue: MLValue[Keywords]): Keywords = new Keywords(mlValue)
}
