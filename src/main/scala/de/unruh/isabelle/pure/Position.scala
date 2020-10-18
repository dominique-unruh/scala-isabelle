package de.unruh.isabelle.pure

import de.unruh.isabelle.control.Isabelle
import de.unruh.isabelle.mlvalue.{MLValue, MLValueWrapper}
import de.unruh.isabelle.mlvalue.MLValue.compileValue

import scala.concurrent.ExecutionContext

// Implicits
import Implicits._

/** Represents a position (ML type `Position.T`) in the Isabelle process.
 *
 * An instance of this class is merely a thin wrapper around an [[mlvalue.MLValue MLValue]],
 * all explanations and examples given for [[Context]] also apply here.
 *
 * An implict [[MLValue.Converter]] can be imported from [[Implicits]]`._`. The representation
 * of a position `pos` as an ML exception is `E_Position pos`.
 */
final class Position private [Position](val mlValue : MLValue[Position]) extends MLValueWrapper[Position] {
  override def toString: String = "position" + mlValue.stateString
}

object Position extends MLValueWrapper.Companion[Position] {
  override protected val mlType = "Position.T"
  override protected val predefinedException: String = "E_Position"

  override protected def instantiate(mlValue: MLValue[Position]): Position = new Position(mlValue)

  class Ops(implicit isabelle: Isabelle, ec: ExecutionContext) extends super.Ops {
    lazy val none: Position = compileValue[Position]("Position.none").retrieveNow
  }

  /** Represents an unspecified position (`Position.none` in ML). */
  def none(implicit isabelle: Isabelle, ec: ExecutionContext): Position = Ops.none

  override protected def newOps(implicit isabelle: Isabelle, ec: ExecutionContext): Ops = new Ops
}