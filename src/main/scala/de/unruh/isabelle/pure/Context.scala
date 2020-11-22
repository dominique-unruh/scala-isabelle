package de.unruh.isabelle.pure

import de.unruh.isabelle.control.{Isabelle, OperationCollection}
import de.unruh.isabelle.mlvalue.MLValue.Converter
import de.unruh.isabelle.mlvalue.{MLFunction, MLValue, MLValueWrapper}

import scala.concurrent.{ExecutionContext, Future}

import Context.Ops

// Implicits
import de.unruh.isabelle.pure.Implicits.theoryConverter
import de.unruh.isabelle.mlvalue.Implicits._

/** Represents a proof context (ML type `Proof.context`) in the Isabelle process.
 *
 * An instance of this class is merely a thin wrapper around an [[mlvalue.MLValue MLValue]],
 * that is, the context is never transferred to the Scala process (which would
 * not be possible because a context cannot be serialized). However, by having
 * this wrapper, a context can be treated as if it were a Scala object (as opposed to
 * a value stored in the Isabelle process).
 *
 * This class is compatible with the [[mlvalue.MLValue MLValue]] mechanism. That is, from a [[Context]] `context`,
 * we can create an [[mlvalue.MLValue MLValue]][[[Context]]] by `[[mlvalue.MLValue MLValue]](context)`, and we can get a [[Context]] back
 * using `.[[mlvalue.MLValue.retrieve retrieve]]`/`.[[mlvalue.MLValue.retrieveNow retrieveNow]]`. This conversion is needed
 * if we want to pass contexts to (or return contexts from) ML functions compiled using [[mlvalue.MLValue.compileFunction[D,R]* MLValue.compileFunction]].
 * For example, say `countTheorems : Proof.context -> int` is an ML function, then we can compile it using
 * {{{
 * val countTheorems = MLValue.compileFunction[Context,Int]("countTheorems")
 * }}}
 * and invoke it as
 * {{{
 * val num : Int = countTheorems(context).retrieveNow  // where context : Context
 * }}}
 * Make sure to import [[de.unruh.isabelle.pure.Implicits]]`._` for the [[mlvalue.MLValue MLValue]]-related functions to work.
 *
 * Not that contexts (being [[mlvalue.MLValue MLValue]]s), are internally futures and may still fail.
 * To make sure a [[Context]] actually contains a value, use, e.g., [[Context.force]].
 *
 * An implict [[mlvalue.MLValue.Converter MLValue.Converter]] can be imported from [[Implicits]]`._`. The ML type is `Proof.context`
 * and the representation of a context `ctxt` as an ML exception is `E_Context ctxt`.
 * */
final class Context private [Context](val mlValue : MLValue[Context]) extends MLValueWrapper[Context] {
  /**
   * Returns "context", "context (computing)", or "context (failed)" depending on
   * whether this context is ready, still being computed, or computation has thrown an exception.
   */
  override def toString: String = "context" + mlValue.stateString
//  override def await: Unit = mlValue.await
//  override def someFuture: Future[Any] = mlValue.someFuture

  /** Returns the theory underlying this context. */
  def theoryOf(implicit isabelle: Isabelle, executionContext: ExecutionContext) : Theory = Ops.theoryOf(this).retrieveNow

  /** Sets the "inner syntax mode" of the context.
   * This affects, e.g., parsing of terms/types.
   * (Corresponds loosely to Proof_Context.set_mode in Isabelle/ML.)
   *
   * @param mode the mode to enable
   * @return a new context with the corresponding mode
   */
  def setMode(mode: Context.Mode)
             (implicit isabelle: Isabelle, executionContext: ExecutionContext) : Context =
    Ops.setMode(this, mode.id).retrieveNow
}

object Context extends MLValueWrapper.Companion[Context] {
  override protected def newOps(implicit isabelle: Isabelle, ec: ExecutionContext): Ops = new Ops()
  //noinspection TypeAnnotation
  protected class Ops(implicit val isabelle: Isabelle, ec: ExecutionContext) extends super.Ops {
    import MLValue.compileFunction

    lazy val contextFromTheory : MLFunction[Theory, Context] =
      compileFunction[Theory, Context]("Proof_Context.init_global")

    lazy val theoryOf = compileFunction[Context, Theory]("Proof_Context.theory_of")

    lazy val setMode = compileFunction[Context, Int, Context](s"""fn (ctxt, id) => Proof_Context.set_mode
       (case id of ${Mode.schematic.id} => Proof_Context.mode_schematic
                 | ${Mode.pattern.id} => Proof_Context.mode_pattern
                 | ${Mode.abbrev.id} => Proof_Context.mode_abbrev) ctxt""")
  }

  /** Initializes a new context from the Isabelle theory `theory` */
  def apply(theory: Theory)(implicit isabelle: Isabelle, ec: ExecutionContext): Context = {
    val mlCtxt : MLValue[Context] = Ops.contextFromTheory(theory.mlValue)
    new Context(mlCtxt)
  }

  /** Initializes a new context from the Isabelle theory `theory`.
   * @param name full name of the theory (see [[Theory.apply(name:* Theory.apply(String)]] for details)
   **/
  def apply(name: String)(implicit isabelle: Isabelle, ec: ExecutionContext) : Context =
    Context(Theory(name))

  override protected val mlType: String = "Proof.context"
  override protected val predefinedException: String = "E_Context"

  override protected def instantiate(mlValue: MLValue[Context]): Context = new Context(mlValue)

  /** Inner syntax mode of a context. (Corresponds to Proof_Context.mode in Isabelle/ML.)
   * Possible values are [[Mode.pattern]], [[Mode.schematic]], [[Mode.abbrev]]. */
  sealed abstract class Mode(private[Context] val id: Int)
  object Mode {
    /** "pattern binding schematic variables" according to Isabelle source comments */
    case object pattern extends Mode(1)
    /** "term referencing loose schematic variables" according to Isabelle source comments */
    case object schematic extends Mode(2)
    /** "abbrev mode – no normalization" according to Isabelle source comments */
    case object abbrev extends Mode(3)
  }
}
