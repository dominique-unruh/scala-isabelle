package de.unruh.isabelle.experiments

import java.nio.file.Path

import de.unruh.isabelle.control.{Isabelle, OperationCollection}
import de.unruh.isabelle.control.IsabelleTest.isabelle
import de.unruh.isabelle.mlvalue.MLValue
import de.unruh.isabelle.pure.{Context, Theory, Thm}
import de.unruh.isabelle.pure.Implicits._
import de.unruh.isabelle.mlvalue.Implicits._
import de.unruh.isabelle.mlvalue.MLValue.{compileFunction, compileFunction0}

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag
import scala.util.Random

// Alternative approach: Have the actual converter as a field in this class, pick the type T in this class, copy that type into the converter, and
// use val Header = new MiniConverter("mltype").converter, and use Header.T as the Header type
// The header type could even be a retrievable type
class MiniConverter[T](val mlType: String)(implicit clazz: ClassTag[T]) extends MLValue.Converter[T] with OperationCollection {
//  abstract class T

  private val global = null // hiding

  override def retrieve(value: MLValue[T])(implicit isabelle: Isabelle, ec: ExecutionContext): Future[T] = ???
  override def store(value: T)(implicit isabelle: Isabelle, ec: ExecutionContext): MLValue[T] = ???
  override def exnToValue: String = s"fn ${exceptionName} x => x"
  override def valueToExn: String = exceptionName

  val exceptionName: String = "E_" + clazz.runtimeClass.getSimpleName + "_" + Random.between(0, Long.MaxValue)
  println(exceptionName)

  protected class Ops(implicit isabelle: Isabelle, ec: ExecutionContext) {
    isabelle.executeMLCodeNow(s"exception $exceptionName of ($mlType)")

  }

  override protected def newOps(implicit isabelle: Isabelle, ec: ExecutionContext) = new Ops
}

abstract class Header

//noinspection TypeAnnotation
object ExecuteIsar {
  import scala.concurrent.ExecutionContext.Implicits.global

  val masterDir = isabelle.setup.workingDirectory
  val theoryText =
    """theory Test imports Main begin
      |
      |lemma test: True
      |  by simp
      |
      |end
      |""".stripMargin

  Context.init()
  Theory.init()

  abstract class Header
  implicit val headerConverter = new MiniConverter[Header]("Thy_Header.header")
  headerConverter.init()
  abstract class ToplevelState
  implicit val toplevelStateConverter = new MiniConverter[ToplevelState]("Toplevel.state")
  toplevelStateConverter.init()
  abstract class Transition
  implicit val transitionConverter = new MiniConverter[Transition]("Toplevel.transition")
  transitionConverter.init()

  val script_thy = compileFunction[String, Theory, Theory]("fn (str,thy) => Thy_Info.script_thy Position.none str thy")
  val begin_theory = compileFunction[String, Header, List[Theory], Theory]("fn (path, header, parents) => Resources.begin_theory (Path.explode path) header parents")
  val header_read = compileFunction[String, Header]("Thy_Header.read Position.none")
  val init_toplevel = compileFunction0[ToplevelState]("Toplevel.init_toplevel")
//  val outer_syntax_parse_text1 = compileFunction[Theory, String, Transition](
//    "fn (thy, string) => Outer_Syntax.parse_text thy (K thy) Position.none string |> the_single")
//  val outer_syntax_parse_text_num = compileFunction[Theory, String, Int](
//    "fn (thy, string) => Outer_Syntax.parse_text thy (K thy) Position.none string |> length")
  val command_exception = compileFunction[Boolean, Transition, ToplevelState, ToplevelState](
    "fn (int, tr, st) => Toplevel.command_exception int tr st")
  val toplevel_end_theory = compileFunction[ToplevelState, Theory]("Toplevel.end_theory Position.none")
  val theory_of_state = compileFunction[ToplevelState, Theory]("Toplevel.theory_of")
  val context_of_state = compileFunction[ToplevelState, Context]("Toplevel.context_of")

  val applyTransitions = compileFunction[Theory, Boolean, String, ToplevelState, ToplevelState](
    "fn (thy, int, text, state) => fold (Toplevel.command_exception true) (Outer_Syntax.parse_text thy (K thy) Position.none text) state")

  val theoryName = compileFunction[Boolean, Theory, String](
    "fn (long, thy) => Context.theory_name' {long=long} thy")

  def printTheoryInfo(thy: Theory): Unit = {
    val name = theoryName(true, thy).retrieveNow
    println(s"Theory $name")
  }


  def main(args: Array[String]): Unit = {
    val mainThy = Theory("Main")

    val headerLine = "theory Test imports Main begin"
    val header = header_read(headerLine).force

    val thy0 = begin_theory(MLValue(masterDir.toString),
      header,
      MLValue(List(mainThy))
    ).force.retrieveNow

    val toplevel = init_toplevel().force

    println("header")
    val stateHeaderLine = applyTransitions(MLValue(thy0), MLValue(true), MLValue("theory Test imports Main begin"), toplevel)

    println("lemma")
    val stateLemma = applyTransitions(MLValue(thy0), MLValue(true), MLValue("lemma test: \"1+1=(2::nat)\""), stateHeaderLine)

    println("simp")
    val stateSimp = applyTransitions(MLValue(thy0), MLValue(true), MLValue("  by simp"), stateLemma)

    println("sneak peek at current state")
    printTheoryInfo(theory_of_state(stateSimp).retrieveNow)

    println("end")
    val stateEnd = applyTransitions(MLValue(thy0), MLValue(true), MLValue("end"), stateSimp)

    val finalThy = toplevel_end_theory(stateEnd).retrieveNow.force

    val ctxt = Context(finalThy)
    val thm = Thm(ctxt, "test")
    println(thm.pretty(ctxt))
  }
}
