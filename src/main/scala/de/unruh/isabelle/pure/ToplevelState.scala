package de.unruh.isabelle.pure
import de.unruh.isabelle.control.Isabelle
import de.unruh.isabelle.mlvalue.{MLValue, MLValueWrapper}
import de.unruh.isabelle.mlvalue.MLValue.compileFunction
import de.unruh.isabelle.pure.ToplevelState.Ops
import scala.concurrent.ExecutionContext

// Implicits
import de.unruh.isabelle.pure.Implicits.theoryConverter

final class ToplevelState private (val mlValue: MLValue[ToplevelState]) extends MLValueWrapper[ToplevelState] {

  // TODO: test case
  // TODO document
  def context(implicit isabelle: Isabelle, ec: ExecutionContext): Context =
    Ops.getContext(this).retrieveNow

  // TODO: test case
  // TODO document
  def setContext(context: Context)(implicit isabelle: Isabelle, ec: ExecutionContext): ToplevelState =
    Ops.setContext(this, context).retrieveNow
}

object ToplevelState extends MLValueWrapper.Companion[ToplevelState] {
  override protected val mlType: String = "Toplevel.state"
  override protected val predefinedException: String = "E_ToplevelState"
  override protected def instantiate(mlValue: MLValue[ToplevelState]): ToplevelState = new ToplevelState(mlValue)

  // TODO: test case
  // TODO document
  def apply(theory: Theory)(implicit isabelle: Isabelle, ec: ExecutionContext): ToplevelState =
    Ops.theoryToplevel(theory).retrieveNow

  override protected def newOps(implicit isabelle: Isabelle, ec: ExecutionContext): Ops = new Ops
  //noinspection TypeAnnotation
  protected class Ops(implicit isabelle: Isabelle, ec: ExecutionContext) extends super.Ops {
    lazy val getContext = compileFunction[ToplevelState, Context]("Toplevel.context_of")
    lazy val setContext = compileFunction[ToplevelState, Context, ToplevelState](
      "fn (st, ctxt) => Toplevel.command_exception true (Toplevel.proof (K ctxt) Toplevel.empty) st")
    lazy val theoryToplevel = compileFunction[Theory, ToplevelState]("Toplevel.theory_toplevel")
  }
}