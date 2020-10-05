package de.unruh.isabelle.pure

import de.unruh.isabelle.mlvalue.MLValue

// DOCUMENT
final class Keywords private (val mlValue: MLValue[Keywords]) extends MLValueWrapper[Keywords]

object Keywords extends MLValueWrapper.Companion[Keywords] {
  override protected val mlType: String = "Thy_Info.keywords"
  override protected def instantiate(mlValue: MLValue[Keywords]): Keywords = new Keywords(mlValue)
}
