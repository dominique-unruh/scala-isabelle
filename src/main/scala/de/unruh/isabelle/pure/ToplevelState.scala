package de.unruh.isabelle.pure
import de.unruh.isabelle.control.Isabelle
import de.unruh.isabelle.mlvalue.{MLValue, MLValueWrapper, Version}
import de.unruh.isabelle.mlvalue.MLValue.{compileFunction, compileFunction0}
import de.unruh.isabelle.pure.ToplevelState.Ops

// Implicits
import de.unruh.isabelle.pure.Implicits.theoryConverter
import de.unruh.isabelle.mlvalue.Implicits._

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

  /** Returns the proof level of this state (number of opened blocks, ML function `Toplevel.level`).
   * Zero when outside of proofs.
   * For Skipped_Proof, this is instead the depth (plus one).
   */
  def proofLevel(implicit isabelle: Isabelle): Int = Ops.getProofLevel(this).retrieveNow


  /** Returns whether the state is in top-level mode, meaning outside a theory (before "theory .. begin" or after "end"). */
  def isTopLevelMode(implicit isabelle: Isabelle): Boolean = Ops.isTopLevelMode(this).retrieveNow
  /** Returns whether the state is in theory mode, global or local. */
  def isTheoryMode(implicit isabelle: Isabelle): Boolean = Ops.isTheoryMode(this).retrieveNow
  /** Returns whether the state is in local theory mode. */
  def isLocalTheoryMode(implicit isabelle: Isabelle): Boolean = Ops.isLocalTheoryMode(this).retrieveNow
  /** Returns whether the state is in proof (non-skipped) mode. */
  def isProofMode(implicit isabelle: Isabelle): Boolean = Ops.isProofMode(this).retrieveNow
  /** Returns whether the state is in skipped-proof mode. */
  def isSkippedMode(implicit isabelle: Isabelle): Boolean = Ops.isSkippedMode(this).retrieveNow
  /** Returns whether the state is after a theory "end". */
  def isEndTheory(implicit isabelle: Isabelle): Boolean = Ops.isEndTheory(this).retrieveNow
  /** Returns the mode of the state, one of: top-level/theory/local-theory/proof/skipped-proof. */
  def mode(implicit isabelle: Isabelle): String = {
    if (isTopLevelMode) "top-level"
    else if (isTheoryMode) { if (isLocalTheoryMode) "local-theory" else "theory" }
    else if (isProofMode) "proof"
    else if (isSkippedMode) "skipped-proof"
    else throw new RuntimeException("Unknown ToplevelState mode.")
  }

  /** Description of the current proof state (often starting like "proof (prove) goal (1 subgoal):"). */
  def proofStateDescription(implicit isabelle: Isabelle): String = Ops.proofStateDescription(this).retrieveNow
  /** Description of the current local theory, like "theory Foo" or "locale foo", or "class foo = ...". */
  def localTheoryDescription(implicit isabelle: Isabelle): String = Ops.localTheoryDescription(this).retrieveNow
}

object ToplevelState extends MLValueWrapper.Companion[ToplevelState] {
  override protected val mlType: String = "Toplevel.state"
  override protected val predefinedException: String = "E_ToplevelState"
  override protected def instantiate(mlValue: MLValue[ToplevelState]): ToplevelState = new ToplevelState(mlValue)

  /** Initializes a new toplevel state based on the theory `theory`. (ML function `Toplevel.theory_toplevel`). */
  def apply(theory: Theory)(implicit isabelle: Isabelle): ToplevelState =
    Ops.theoryToplevel(theory).retrieveNow
  /** Initializes a new empty toplevel state (based on the 'Pure' theory). */
  def apply()(implicit isabelle: Isabelle) : ToplevelState = Ops.initTopLevel().retrieveNow

  override protected def newOps(implicit isabelle: Isabelle): Ops = new Ops
  //noinspection TypeAnnotation
  protected class Ops(implicit isabelle: Isabelle) extends super.Ops {
    lazy val getContext = compileFunction[ToplevelState, Context]("Toplevel.context_of")
    lazy val getTheory = compileFunction[ToplevelState, Theory]("Toplevel.theory_of")
    lazy val getProofLevel = compileFunction[ToplevelState, Int]("Toplevel.level")

    lazy val isTopLevelMode = compileFunction[ToplevelState, Boolean]("Toplevel.is_toplevel")
    lazy val isTheoryMode = compileFunction[ToplevelState, Boolean]("Toplevel.is_theory")
    lazy val isProofMode = compileFunction[ToplevelState, Boolean]("Toplevel.is_proof")
    lazy val isSkippedMode = compileFunction[ToplevelState, Boolean]("Toplevel.is_skipped_proof")
    lazy val isEndTheory = compileFunction[ToplevelState, Boolean]("Toplevel.is_end_theory")
    lazy val isLocalTheoryMode = compileFunction[ToplevelState, Boolean](
      "fn (st) => Context.cases (K false) (K true) (Toplevel.generic_theory_of st)")

    lazy val proofStateDescription = compileFunction[ToplevelState, String](
      "fn (st) => YXML.content_of (Toplevel.string_of_state st)")
    lazy val localTheoryDescription = compileFunction[ToplevelState, String](
      "fn (st) => YXML.content_of(Pretty.string_of(" +
        "(Pretty.block o Pretty.breaks) (Toplevel.pretty_context st)" +
       "))")

    lazy val initTopLevel =
       if (Version.from2023)
        compileFunction0[ToplevelState]("Toplevel.make_state o NONE")
      else
        compileFunction0[ToplevelState]("Toplevel.init_toplevel")
    lazy val theoryToplevel =
      if (Version.from2023)
        compileFunction[Theory, ToplevelState]("Toplevel.make_state o SOME")
      else
        compileFunction[Theory, ToplevelState]("Toplevel.theory_toplevel")
  }
}
