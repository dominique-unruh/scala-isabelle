package de.unruh.isabelle.experiments

import java.nio.file.Path

import de.unruh.isabelle.control.{Isabelle, IsabelleException, OperationCollection}
import de.unruh.isabelle.control.IsabelleTest.isabelle
import de.unruh.isabelle.experiments
import de.unruh.isabelle.mlvalue.{AdHocConverter, FutureValue, MLValue}
import de.unruh.isabelle.pure.{Context, Theory, Thm}
import de.unruh.isabelle.pure.Implicits._
import de.unruh.isabelle.mlvalue.Implicits._
import de.unruh.isabelle.mlvalue.MLValue.{compileFunction, compileFunction0}

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag
import scala.util.Random



//noinspection TypeAnnotation
object ExecuteIsar {
  import scala.concurrent.ExecutionContext.Implicits.global

  val masterDir = isabelle.setup.workingDirectory
  val theoryText =
    """theory Test imports Main begin
      |
      |lemma test: "1+1=(2::nat)"
      |(* Test *)
      |  by simp
      |
      |end
      |""".stripMargin

  Context.init()
  Theory.init()

  object Header extends AdHocConverter("Thy_Header.header")
  object RuntimeError extends AdHocConverter("Runtime.error")
  object ToplevelState extends AdHocConverter("Toplevel.state")
  object Transition extends AdHocConverter("Toplevel.transition")

  val script_thy = compileFunction[String, Theory, Theory]("fn (str,thy) => Thy_Info.script_thy Position.none str thy")
  val begin_theory = compileFunction[String, Header.T, List[Theory], Theory]("fn (path, header, parents) => Resources.begin_theory (Path.explode path) header parents")
  val header_read = compileFunction[String, Header.T]("Thy_Header.read Position.none")
  val init_toplevel = compileFunction0[ToplevelState.T]("Toplevel.init_toplevel")
  val command_exception = compileFunction[Boolean, Transition.T, ToplevelState.T, ToplevelState.T](
    "fn (int, tr, st) => Toplevel.command_exception int tr st")
  val command_errors = compileFunction[Boolean, Transition.T, ToplevelState.T, (List[RuntimeError.T], Option[ToplevelState.T])](
    "fn (int, tr, st) => Toplevel.command_errors int tr st")
  val toplevel_end_theory = compileFunction[ToplevelState.T, Theory]("Toplevel.end_theory Position.none")
  val theory_of_state = compileFunction[ToplevelState.T, Theory]("Toplevel.theory_of")
  val context_of_state = compileFunction[ToplevelState.T, Context]("Toplevel.context_of")

  val parse_text = compileFunction[Theory, String, List[(Transition.T, String)]](
    """fn (thy, text) => let
      |  val transitions = Outer_Syntax.parse_text thy (K thy) Position.start text
      |  fun addtext symbols [tr] =
      |        [(tr, implode symbols)]
      |    | addtext _ [] = []
      |    | addtext symbols (tr::nextTr::trs) = let
      |        val (this,rest) = Library.chop (Position.distance_of (Toplevel.pos_of tr, Toplevel.pos_of nextTr) |> Option.valOf) symbols
      |        in (tr, implode this) :: addtext rest (nextTr::trs) end
      |  in addtext (Symbol.explode text) transitions end""".stripMargin)

  val applyTransitions = compileFunction[Theory, Boolean, String, ToplevelState.T, ToplevelState.T](
    "fn (thy, int, text, state) => fold (Toplevel.command_exception true) (Outer_Syntax.parse_text thy (K thy) Position.none text) state")

  val theoryName = compileFunction[Boolean, Theory, String](
    "fn (long, thy) => Context.theory_name' {long=long} thy")

  val toplevel_string_of_state = compileFunction[ToplevelState.T, String]("Toplevel.string_of_state")

  def printTheoryInfo(thy: Theory): Unit = {
    val name = theoryName(true, thy).retrieveNow
    println(s"Theory $name")
  }


  def main(args: Array[String]): Unit = {
    val mainThy = Theory("Main")

    val header = header_read(theoryText).force.retrieveNow

    val thy0 = begin_theory(masterDir.toString, header, List(mainThy)).force.retrieveNow

    var toplevel = init_toplevel().force.retrieveNow

    for ((transition, text) <- parse_text(thy0, theoryText).force.retrieveNow) {
      println(s"Transition: $text")
      toplevel = command_exception(true, transition, toplevel).retrieveNow.force
//      val (errors,maybeToplevel) = command_errors(false, transition, toplevel).force.retrieveNow
//      println(errors)
//      maybeToplevel match {
//        case Some(t) =>
//          toplevel = t
//          println("State: " + toplevel_string_of_state(toplevel).retrieveNow)
//          try {
//            val ctxt = context_of_state(toplevel).retrieveNow.force
//            println("Thm: "+Thm(ctxt, "test").pretty(ctxt))
//          } catch {
//            case _ : IsabelleException => println("No thm")
//          }
//        case None => println("No new toplevel state")
//      }
    }

    val finalThy = toplevel_end_theory(toplevel).retrieveNow.force

    val ctxt = Context(finalThy)
    val thm = Thm(ctxt, "test")
    println(thm.pretty(ctxt))
  }
}
