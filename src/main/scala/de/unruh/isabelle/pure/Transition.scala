package de.unruh.isabelle.pure

import de.unruh.isabelle.control.Isabelle
import de.unruh.isabelle.mlvalue.Implicits.listConverter
import de.unruh.isabelle.mlvalue.{MLValue, MLValueWrapper}
import Transition.Ops

import scala.language.postfixOps
import scala.concurrent.duration.Duration

// Implicits
import de.unruh.isabelle.mlvalue.Implicits._
import de.unruh.isabelle.pure.Implicits.theoryConverter

/** Represents a transition between proof states (ML type `Toplevel.transition`) in the Isabelle process.
  *
  * Use `Transition.parseOuterSyntax` to parse Isar source code into into a list of transitions.
  *
  * An instance of this class is merely a thin wrapper around an [[mlvalue.MLValue MLValue]].
  */
final class Transition private[Transition] (val mlValue: MLValue[Transition])
    extends MLValueWrapper[Transition] {

  /** Returns the command name (ML function `Toplevel.name_of`).
   *
   * Examples: "theory", "section", "lemma", "by", "using", "definition", 'locale", "record", "end".
   * The name is "<ignored>" if and only if isIgnored is true.
   * The name is "<malformed>" if and only if isMalformed is true.
   */
  def name(implicit isabelle: Isabelle): String = Ops.getName(this).retrieveNow
  /** Returns the position of this transition's command name. */
  def position(implicit isabelle: Isabelle): Position = Ops.getPosition(this).retrieveNow

  /** Returns whether this transition introduces a theory ("theory .. begin", ML function `Toplevel.is_init`).
   *
   * Init transitions can only be applied to toplevel states in theory mode.
   */
  def isInit(implicit isabelle: Isabelle): Boolean = Ops.isInit(this).retrieveNow
  /** Returns whether the transition is just whitespace, comments, text (ML function `Toplevel.is_ignored`). */
  def isIgnored(implicit isabelle: Isabelle): Boolean = Ops.isIgnored(this).retrieveNow
  /** Returns true if the transition is a parsing error  (ML function `Toplevel.is_malformed`). */
  def isMalformed(implicit isabelle: Isabelle): Boolean = Ops.isMalformed(this).retrieveNow

  /** Execute the transition on a TopLevelState (ML function `Toplevel.command_exception`).
   *
   * @param state The state on which to execute the transition.
   * @param timeout Maximum duration (otherwise the command is aborted).
   * @param interactive Whether the command is run interactively (as in jEdit) or in a batch (as in `isabelle build`).
   */
  def execute(
      state: ToplevelState,
      timeout: Option[Duration] = None,
      interactive: Boolean = false
  )(implicit isabelle: Isabelle): ToplevelState = {
    (timeout match {
      case Some(value) => Ops.commandExceptionWithTimeout(value.toMillis, interactive, this, state)
      case None => Ops.commandException(interactive, this, state)
    }).retrieveNow.force
  }
}

object Transition extends MLValueWrapper.Companion[Transition] {
  override protected val mlType: String              = "Toplevel.transition"
  override protected val predefinedException: String = "E_Transition"

  /** Parse Isar outer syntax into transitions (ML function `Outer_Syntax.parse_text`).
   *
   * @param thy Theory in which the source code is to be parsed.
   *    For example: `Theory.mergeTheories("Foo", endTheory=false, List(Theory("Main")))`
   *
   * @return list of pairs (transition, corresponding full text of transition).
   */
  def parseOuterSyntax(thy: Theory, source: String)(implicit isabelle: Isabelle) : List[(Transition, String)] =
    Ops.parseOuterSyntax(thy, source).retrieveNow

  // noinspection TypeAnnotation
  protected final class Ops(implicit isabelle: Isabelle) extends super.Ops {
    import MLValue.compileFunction
    lazy val getName = compileFunction[Transition, String]("Toplevel.name_of")
    lazy val getPosition = compileFunction[Transition, Position]("Toplevel.pos_of")
    lazy val isInit = compileFunction[Transition, Boolean]("Toplevel.is_init")
    lazy val isIgnored = compileFunction[Transition, Boolean]("Toplevel.is_ignored")
    lazy val isMalformed = compileFunction[Transition, Boolean]("Toplevel.is_malformed")
    lazy val commandException = compileFunction[Boolean, Transition, ToplevelState, ToplevelState](
      "fn (int, tr, st) => Toplevel.command_exception int tr st"
    )
    lazy val commandExceptionWithTimeout =
      compileFunction[Long, Boolean, Transition, ToplevelState, ToplevelState](
        """fn (timeout, int, tr, st) =>
          |  Timeout.apply (Time.fromMilliseconds timeout) Toplevel.command_exception int tr st
        """.stripMargin
      )

    // Calls Outer_Syntax.parse_text and pairs each transition with the text that it corresponds to.
    lazy val parseOuterSyntax = compileFunction[Theory, String, List[(Transition, String)]](
      """fn (thy, source) => let
        |  val transitions = Outer_Syntax.parse_text thy (K thy) Position.start source
        |  fun addtext symbols [tr]              = [(tr, implode symbols)]
        |    | addtext _       []                = []
        |    | addtext symbols (tr::nextTr::trs) = let
        |        val (this,rest) = Library.chop (Position.distance_of (Toplevel.pos_of tr, Toplevel.pos_of nextTr) |> Option.valOf) symbols
        |        in (tr, implode this) :: addtext rest (nextTr::trs) end
        |  in addtext (Symbol.explode source) transitions end
      """.stripMargin
    )
  }
  override protected def newOps(implicit isabelle: Isabelle): Ops = new Ops

  override protected def instantiate(mlValue: MLValue[Transition]): Transition =
    new Transition(mlValue)
}

