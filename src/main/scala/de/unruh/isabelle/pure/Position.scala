package de.unruh.isabelle.pure

import de.unruh.isabelle.mlvalue.MLValue

// DOCUMENT
final class Position private [Position](val mlValue : MLValue[Position]) extends MLValueWrapper[Position] {
  override def toString: String = "position" + mlValue.stateString
}

object Position extends MLValueWrapper.Companion[Position] {
  override protected val mlType = "Position.T"

  override protected def instantiate(mlValue: MLValue[Position]): Position = new Position(mlValue)
}