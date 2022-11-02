package de.unruh.isabelle.pure
import de.unruh.isabelle.control.Isabelle
import de.unruh.isabelle.mlvalue.{MLValue, MLValueWrapper}
import de.unruh.isabelle.mlvalue.MLValue.compileFunction
import de.unruh.isabelle.pure.ToplevelState.Ops

// Implicits
import de.unruh.isabelle.pure.Implicits.theoryConverter


/** Represents a toplevel state (the state when processing an Isar .thy document). ML type: `Toplevel.state`
 *
 * An instance of this class is merely a thin wrapper around an [[mlvalue.MLValue MLValue]],
 * all explanations and examples given for [[Context]] also apply here.
 *
 * An implict [[mlvalue.MLValue.Converter MLValue.Converter]] can be imported from [[Implicits]]`._`. The representation
 * of a toplevel state `state` as an ML exception is `E_ToplevelState state`.
 */
final class ToplevelState private (val mlValue: MLValue[ToplevelState]) extends MLValueWrapper[ToplevelState] {
  /** Returns the theory corresponding to this toplevel state (ML function `Toplevel.theory_of`). */
  def theory(implicit isabelle: Isabelle): Theory =
    Ops.getTheory(this).retrieveNow

  /** Returns the proof context corresponding to this toplevel state (ML function `Toplevel.context_of`). */
  def context(implicit isabelle: Isabelle): Context =
    Ops.getContext(this).retrieveNow
}

object ToplevelState extends MLValueWrapper.Companion[ToplevelState] {
  override protected val mlType: String = "Toplevel.state"
  override protected val predefinedException: String = "E_ToplevelState"
  override protected def instantiate(mlValue: MLValue[ToplevelState]): ToplevelState = new ToplevelState(mlValue)

  /** Initializes a new toplevel state based on the theory `theory`. (ML function `Toplevel.theory_toplevel`). */
  def apply(theory: Theory)(implicit isabelle: Isabelle): ToplevelState =
    Ops.theoryToplevel(theory).retrieveNow

  override protected def newOps(implicit isabelle: Isabelle): Ops = new Ops
  //noinspection TypeAnnotation
  protected class Ops(implicit isabelle: Isabelle) extends super.Ops {
    lazy val getContext = compileFunction[ToplevelState, Context]("Toplevel.context_of")
    lazy val getTheory = compileFunction[ToplevelState, Theory]("Toplevel.theory_of")
    lazy val theoryToplevel = compileFunction[Theory, ToplevelState]("Toplevel.theory_toplevel")
  }
}
