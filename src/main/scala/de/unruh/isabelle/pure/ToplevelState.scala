package de.unruh.isabelle.pure
import java.nio.file.Path

import de.unruh.isabelle.control.Isabelle
import de.unruh.isabelle.mlvalue.MLValue
import de.unruh.isabelle.mlvalue.MLValue.compileFunction

import scala.concurrent.ExecutionContext

final class ToplevelState private (val mlValue: MLValue[ToplevelState]) extends MLValueWrapper[ToplevelState] {

}

object ToplevelState extends MLValueWrapper.Companion[ToplevelState] {
  override protected val mlType: String = "Toplevel.state"
  override protected def instantiate(mlValue: MLValue[ToplevelState]): ToplevelState = new ToplevelState(mlValue)
}