package de.unruh.isabelle.experiments

import java.nio.file.{Files, Path}

import de.unruh.isabelle.control.Isabelle
import de.unruh.isabelle.control.Isabelle.{DInt, DList, DString}
import de.unruh.isabelle.control.Isabelle.{setup}
//import de.unruh.isabelle.experiments.ExecuteIsar.ScalaCommand.{Code, Empty, Preamble}
import de.unruh.isabelle.experiments.ExecuteIsar._
import de.unruh.isabelle.experiments.ScalaTransition.Info
import de.unruh.isabelle.mlvalue.{AdHocConverter, MLRetrieveFunction, MLValue}
import de.unruh.isabelle.mlvalue.MLValue.{compileFunction, compileFunction0, compileValue}
import de.unruh.isabelle.pure.{Context, Theory, TheoryHeader, Thm, ToplevelState}

import scala.concurrent.Future
import scala.reflect.runtime.currentMirror
import scala.tools.reflect.ToolBox

// Implicits
import de.unruh.isabelle.control.IsabelleTest.isabelle
import de.unruh.isabelle.mlvalue.Implicits._
import de.unruh.isabelle.pure.Implicits._
import de.unruh.isabelle.control.Isabelle.executionContext


object RuntimeError extends AdHocConverter("Runtime.error")
//object ToplevelState extends AdHocConverter("Toplevel.state")
object ProofState extends AdHocConverter("Proof.state")
object Transition extends AdHocConverter("Toplevel.transition")
object Tactic extends AdHocConverter("tactic")
object MethodText extends AdHocConverter("Method.text")


abstract class ScalaTransition {
  def apply(state: ToplevelState, info: Info) : ToplevelState
}

object ScalaTransition {
  final case class Info()
}

object TestTransition extends ScalaTransition {
  override def apply(state: ToplevelState, info: Info): ToplevelState = {
    println(toplevel_string_of_state(state).retrieveNow)
    assert(is_proof(state).retrieveNow)
    val proof = proof_of(state).retrieveNow.force
    val thy = theory_of_state(state).retrieveNow.force
//    val skip_proof_tac = compileFunction[Context, Int, Tactic.T]("Skip_Proof.cheat_tac")
    val simp_method =
      compileValue[MethodText.T]("Method.Basic (fn ctxt => Method.SIMPLE_METHOD' (simp_tac ctxt))").retrieveNow.force
    val apply_method =
      compileFunction[MethodText.T, ProofState.T, ProofState.T]("fn (m,st) => Proof.apply (m, Position.no_range) st |> Seq.the_result \"apply_method\"").force

    val newProof = apply_method(simp_method, proof).retrieveNow.force
    val update_proof =
      compileFunction[ProofState.T, ToplevelState, ToplevelState](
        "fn (proof, st) => Toplevel.command_exception false (Toplevel.proof (K proof) Toplevel.empty) st")
    val newState = update_proof(newProof, state).force.retrieveNow
    println(toplevel_string_of_state(newState).retrieveNow)
    newState
  }
}

//noinspection TypeAnnotation
object ExecuteIsar {
  val theoryManager = new TheoryManager {
    override def getTheorySource(name: String): TheoryManager.Source = name match {
//      case "ScalaKeywords" =>
//        val path = isabelle.setup.workingDirectory.resolve("ScalaKeywords.thy")
//        val text = Files.readString(path)
//        TheoryManager.Text(text, path)
      case _ =>
        super.getTheorySource(name)
    }
    override def getHeader(source: TheoryManager.Source)(implicit isabelle: Isabelle): TheoryHeader = {
      val global = null
      val header = super.getHeader(source)
//      addScalaKeyword(header).retrieveNow
      header
    }
  }

//  Theory.registerSessionDirectoriesNow("ScalaKeywords" -> setup.workingDirectory)

  //  val masterDir = isabelle.setup.workingDirectory
  //  val masterDir = Paths.get("/tmp/fsdfasdfasdofji/sdfasdf")
  val theorySource = TheoryManager.Text(
    /*"""theory Test imports ScalaKeywords.ScalaKeywords Main begin
      |preamble "import de.unruh.isabelle.experiments._"
      |lemma test: "1+1=(2::nat)"
      |  scala TestTransition
      |  by -
      |
      |end
      |""".stripMargin*/
    """theory Test imports Main begin lemma test: "1+1=(2::nat)" by simp end""",
    setup.workingDirectory.resolve("Test.thy"))

//  Context.init()
//  Theory.init()

//  val addScalaKeyword = compileFunction[TheoryHeader, TheoryHeader](
//    """fn {name,imports,keywords} => {name=name,imports=imports,keywords=(("scala", Position.none), (("diag", []), []))::(("preamble", Position.none), (("diag", []), []))::keywords}""")
  val script_thy = compileFunction[String, Theory, Theory]("fn (str,thy) => Thy_Info.script_thy Position.none str thy")
  val init_toplevel = compileFunction0[ToplevelState]("Toplevel.init_toplevel")
  val is_proof = compileFunction[ToplevelState, Boolean]("Toplevel.is_proof")
  val proof_of = compileFunction[ToplevelState, ProofState.T]("Toplevel.proof_of")
  val command_exception = compileFunction[Boolean, Transition.T, ToplevelState, ToplevelState](
    "fn (int, tr, st) => Toplevel.command_exception int tr st")
  val command_errors = compileFunction[Boolean, Transition.T, ToplevelState, (List[RuntimeError.T], Option[ToplevelState])](
    "fn (int, tr, st) => Toplevel.command_errors int tr st")
  val toplevel_end_theory = compileFunction[ToplevelState, Theory]("Toplevel.end_theory Position.none")
  val theory_of_state = compileFunction[ToplevelState, Theory]("Toplevel.theory_of")
  val context_of_state = compileFunction[ToplevelState, Context]("Toplevel.context_of")
  val name_of_transition = compileFunction[Transition.T, String]("Toplevel.name_of")

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

  val applyTransitions = compileFunction[Theory, Boolean, String, ToplevelState, ToplevelState](
    "fn (thy, int, text, state) => fold (Toplevel.command_exception true) (Outer_Syntax.parse_text thy (K thy) Position.none text) state")

  val theoryName = compileFunction[Boolean, Theory, String](
    "fn (long, thy) => Context.theory_name' {long=long} thy")

  val toplevel_string_of_state = compileFunction[ToplevelState, String]("Toplevel.string_of_state")

//  val scalaKeywordsThy = Theory("ScalaKeywords.ScalaKeywords").force
//  val scalaKeywordsStruct = await(scalaKeywordsThy.importMLStructure("ScalaKeywords"))
//  val initialize_data = compileFunction[ToplevelState,ToplevelState](s"$scalaKeywordsStruct.initialize_data")

  trait ScalaCommand
/*  implicit object ScalaCommand extends MLValue.Converter[ScalaCommand] {
    val global = null

    final case class Code(code: String) extends ScalaCommand
    final case class Preamble(code: String) extends ScalaCommand
    final case object Empty extends ScalaCommand

    override def mlType(implicit isabelle: Isabelle): String = s"$scalaKeywordsStruct.scala_command"
    override def retrieve(value: MLValue[ScalaCommand])(implicit isabelle: Isabelle): Future[ScalaCommand] =
      for (data <- retrieveScalaCommand(value))
        yield data match {
          case DInt(0) => Empty
          case DList(DInt(1), DString(str)) => ScalaCommand.Code(str)
          case DList(DInt(2), DString(str)) => ScalaCommand.Preamble(str)
        }
    override def store(value: ScalaCommand)(implicit isabelle: Isabelle): MLValue[ScalaCommand] = ???
    override def exnToValue(implicit isabelle: Isabelle): String =
      s"fn $scalaKeywordsStruct.E_ScalaCommand sc => sc"
    override def valueToExn(implicit isabelle: Isabelle): String = s"$scalaKeywordsStruct.E_ScalaCommand"
  }*/

/*  val retrieveScalaCommand = MLRetrieveFunction[ScalaCommand](
    s"fn $scalaKeywordsStruct.Empty => DInt 0 | $scalaKeywordsStruct.Code str => DList[DInt 1, DString str] | $scalaKeywordsStruct.Preamble str => DList[DInt 2, DString str]")*/

//  val retrieve_command = compileFunction[ToplevelState,ScalaCommand](s"$scalaKeywordsStruct.retrieve_command")


  def printTheoryInfo(thy: Theory): Unit = {
    val name = theoryName(true, thy).retrieveNow
    println(s"Theory $name")
  }

  val toolbox = currentMirror.mkToolBox()

  def main(args: Array[String]): Unit = {
//    val mainThy = Theory("Main")

//    val thySource = TheoryManager.Text(theoryText, masterDir.resolve("Test.thy"))
//    val header = TheoryManager.getHeader(thySource)
//    val thy0 = begin_theory(masterDir, header, header.imports.map(theoryManager.getTheory)).retrieveNow.force
    val thy0 = theoryManager.beginTheory(theorySource)

    var toplevel = init_toplevel().force.retrieveNow
    val preamble = new StringBuilder()
    var initialized = false
    for ((transition, text) <- parse_text(thy0, theorySource.text).force.retrieveNow) {
      println(s"""Transition: "${text.strip}"""")
      toplevel = command_exception(true, transition, toplevel).retrieveNow.force

/*      val cmd = retrieve_command(toplevel).retrieveNow
      cmd match {
        case Empty =>
        case Code(scala) =>
          val scala2 = s"$preamble\n{ {\n$scala\n} : _root_.de.unruh.isabelle.experiments.ScalaTransition }"
          println("Scala: " + scala2.replace('\n',' '))
          val transition = toolbox.eval(toolbox.parse(scala2)).asInstanceOf[ScalaTransition]
          val info = ScalaTransition.Info()
          toplevel = transition(toplevel, info)
        case Preamble(code) =>
          preamble.append(code).append('\n')
      }*/
    }

    val finalThy = toplevel_end_theory(toplevel).retrieveNow.force

    val ctxt = Context(finalThy)
    val thm = Thm(ctxt, "test")
    println(thm.pretty(ctxt))
  }
}
